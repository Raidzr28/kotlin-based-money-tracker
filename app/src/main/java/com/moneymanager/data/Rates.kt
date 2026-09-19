package com.moneymanager.data

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/*
 * Exchange rates, from Frankfurter.
 *
 * Free, no key, no rate limit, and published by the European Central Bank. The request carries a
 * base currency code and nothing else: no account, no amount, no identifier. It is the only
 * outbound request this app makes, and it only happens when multi-currency is switched on.
 *
 * No HTTP library. One GET returning one small JSON object does not need Retrofit, and
 * HttpURLConnection plus the framework's own JSONObject are already on every device.
 *
 * Rates are held as micros -- the rate times a million, as a Long -- so conversion is integer
 * arithmetic from end to end. A Double here would be a rounding error in somebody's ledger.
 */

const val RATE_SCALE = 1_000_000L

/**
 * A payment made in a currency that is not the account's own.
 *
 * Carried alongside the converted amount rather than instead of it, because both are true: the
 * card was charged in one currency and the user handed over another.
 */
data class ForeignAmount(
    val minor: Long,
    val currency: String,
    val rateMicros: Long,
)

data class Rates(
    val base: String,
    val date: LocalDate,
    /** Units of each currency per one unit of [base], times [RATE_SCALE]. */
    val perBaseMicros: Map<String, Long>,
    val fetchedAtEpochSecond: Long,
    /** True when the user typed these in rather than the network supplying them. */
    val manual: Boolean = false,
) {
    fun microsFor(currency: String): Long? =
        if (currency == base) RATE_SCALE else perBaseMicros[currency]
}

/**
 * Converts between currencies through the base.
 *
 * Returns null when either side has no known rate, which callers must show as "not converted"
 * rather than substituting the raw number. Displaying 40 euros as 40 dollars because a rate was
 * missing is worse than displaying nothing.
 */
fun convertMinor(amountMinor: Long, from: String, to: String, rates: Rates): Long? {
    if (from == to) return amountMinor
    val fromMicros = rates.microsFor(from) ?: return null
    val toMicros = rates.microsFor(to) ?: return null
    if (fromMicros == 0L) return null
    val inBase = divRound(amountMinor * RATE_SCALE, fromMicros)
    return divRound(inBase * toMicros, RATE_SCALE)
}

/**
 * Integer division that rounds half away from zero, in both directions.
 *
 * Kotlin's `/` truncates toward zero, which would quietly shave a cent off every conversion and
 * shave it the *wrong way* for negative amounts, so spending and income would drift apart.
 */
internal fun divRound(numerator: Long, denominator: Long): Long {
    if (denominator == 0L) return 0L
    val half = denominator.let { if (it < 0) -it else it } / 2
    return if ((numerator >= 0) == (denominator >= 0)) {
        (numerator + half) / denominator
    } else {
        (numerator - half) / denominator
    }
}

/** Parses a Frankfurter `/latest` response. Returns null on anything unexpected. */
fun parseRatesJson(body: String, fetchedAtEpochSecond: Long): Rates? = runCatching {
    val json = JSONObject(body)
    val base = json.getString("base")
    val date = LocalDate.parse(json.getString("date"))
    val ratesJson = json.getJSONObject("rates")
    val micros = buildMap {
        ratesJson.keys().forEach { key ->
            val value = ratesJson.getDouble(key)
            // The only place a Double appears: JSON has no other number type. It becomes an
            // integer immediately and never touches an amount.
            put(key, Math.round(value * RATE_SCALE))
        }
    }
    Rates(base, date, micros, fetchedAtEpochSecond)
}.getOrNull()

/**
 * Holds the last rates seen and knows how to go and get new ones.
 *
 * Cached in preferences so the app converts correctly with the network off, which is the normal
 * state this app is designed for. A stale rate is labelled, never hidden.
 */
class RatesStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("rates", Context.MODE_PRIVATE)

    var baseCurrency: String
        get() = prefs.getString(KEY_BASE, "USD") ?: "USD"
        set(value) = prefs.edit().putString(KEY_BASE, value).apply()

    var multiCurrencyEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    fun cached(): Rates? {
        val body = prefs.getString(KEY_BODY, null) ?: return null
        val at = prefs.getLong(KEY_AT, 0L)
        val parsed = parseRatesJson(body, at) ?: return null
        return parsed.copy(manual = prefs.getBoolean(KEY_MANUAL, false))
    }

    /** True when the cache is older than a day, or empty. */
    fun isStale(): Boolean {
        val at = prefs.getLong(KEY_AT, 0L)
        return (System.currentTimeMillis() / 1000 - at) > 86_400
    }

    /**
     * Fetches fresh rates, or returns the cached ones on any failure.
     *
     * Failure here is ordinary, not exceptional: this app is expected to run with no network.
     * Nothing about a failed lookup should reach the user as an error.
     */
    fun refresh(base: String = baseCurrency): Rates? {
        val body = runCatching {
            val connection = (URL("https://api.frankfurter.app/latest?base=$base")
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
            }
            try {
                if (connection.responseCode != 200) null
                else connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }.getOrNull() ?: return cached()

        val now = System.currentTimeMillis() / 1000
        val parsed = parseRatesJson(body, now) ?: return cached()
        prefs.edit()
            .putString(KEY_BODY, body)
            .putLong(KEY_AT, now)
            .putBoolean(KEY_MANUAL, false)
            .apply()
        return parsed
    }

    /**
     * Overrides one rate by hand.
     *
     * The README asks for this and it is not a nicety: a card issuer's rate is not the ECB's, and
     * the user reconciling a statement needs the number their bank actually used.
     */
    fun setManualRate(currency: String, perBaseMicros: Long) {
        val current = cached() ?: Rates(baseCurrency, LocalDate.now(), emptyMap(), 0)
        val merged = current.perBaseMicros.toMutableMap().apply { put(currency, perBaseMicros) }
        val body = JSONObject().apply {
            put("base", current.base)
            put("date", current.date.toString())
            put("rates", JSONObject().apply {
                merged.forEach { (code, micros) -> put(code, micros.toDouble() / RATE_SCALE) }
            })
        }.toString()
        prefs.edit()
            .putString(KEY_BODY, body)
            .putLong(KEY_AT, System.currentTimeMillis() / 1000)
            .putBoolean(KEY_MANUAL, true)
            .apply()
    }

    private companion object {
        const val KEY_BASE = "base_currency"
        const val KEY_ENABLED = "multi_currency"
        const val KEY_BODY = "rates_body"
        const val KEY_AT = "rates_fetched_at"
        const val KEY_MANUAL = "rates_manual"
    }
}

/** The currencies offered in the picker. Frankfurter publishes these. */
val SUPPORTED_CURRENCIES = listOf(
    "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SGD", "IDR",
    "INR", "MYR", "NZD", "PHP", "THB", "KRW", "SEK", "NOK", "DKK", "ZAR", "BRL", "MXN",
)
