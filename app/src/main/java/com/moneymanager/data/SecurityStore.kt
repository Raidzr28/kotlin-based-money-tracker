package com.moneymanager.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/*
 * The app lock.
 *
 * The PIN is never stored. What is stored is a PBKDF2-SHA256 hash of it with a random 16-byte
 * salt, at an iteration count chosen to make guessing expensive rather than to be fast. Anyone
 * reading the preferences file gets a hash and a salt, which is not a PIN.
 *
 * Deliberately no EncryptedSharedPreferences. Encrypting a salted slow hash adds a dependency and
 * a key-management story to protect something that is already not secret-shaped; the hash is
 * doing that job. What matters far more for a four-digit PIN is the iteration count and the
 * throttle below, because the search space is only ten thousand.
 */
class SecurityStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("security", Context.MODE_PRIVATE)

    // --- Settings --------------------------------------------------------------------------------

    val hasPin: Boolean get() = prefs.contains(KEY_HASH)

    var lockEnabled: Boolean
        get() = hasPin && prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    /** Blanks the window in the app switcher, and blocks screenshots while it is on. */
    var hideInRecents: Boolean
        get() = prefs.getBoolean(KEY_HIDE, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE, value).apply()

    // --- The PIN ---------------------------------------------------------------------------------

    fun setPin(pin: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, salt.encode())
            .putString(KEY_HASH, derive(pin, salt).encode())
            .putBoolean(KEY_ENABLED, true)
            .remove(KEY_FAILED)
            .remove(KEY_LOCKED_UNTIL)
            .apply()
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_HASH).remove(KEY_SALT)
            .remove(KEY_FAILED).remove(KEY_LOCKED_UNTIL)
            .putBoolean(KEY_ENABLED, false)
            .apply()
    }

    /**
     * Checks a PIN and records the attempt.
     *
     * Compared with [MessageDigest.isEqual], which does not return early on the first differing
     * byte. A naive equals leaks how much of the hash matched through timing.
     */
    fun verifyPin(pin: String): Boolean {
        if (lockoutRemainingMillis() > 0) return false
        val salt = prefs.getString(KEY_SALT, null)?.decode() ?: return false
        val stored = prefs.getString(KEY_HASH, null)?.decode() ?: return false
        val ok = MessageDigest.isEqual(stored, derive(pin, salt))
        if (ok) {
            prefs.edit().remove(KEY_FAILED).remove(KEY_LOCKED_UNTIL).apply()
        } else {
            val failed = prefs.getInt(KEY_FAILED, 0) + 1
            val edit = prefs.edit().putInt(KEY_FAILED, failed)
            // Four digits is ten thousand guesses. Without a throttle that is minutes of work, so
            // every attempt past the fifth costs progressively more waiting.
            if (failed >= FREE_ATTEMPTS) {
                val penalty = BASE_PENALTY_MILLIS shl (failed - FREE_ATTEMPTS).coerceAtMost(6)
                edit.putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + penalty)
            }
            edit.apply()
        }
        return ok
    }

    /** Milliseconds until another attempt is allowed. Zero when the user may try now. */
    fun lockoutRemainingMillis(): Long =
        (prefs.getLong(KEY_LOCKED_UNTIL, 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    fun failedAttempts(): Int = prefs.getInt(KEY_FAILED, 0)

    private fun derive(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance(ALGORITHM)
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS))
            .encoded

    private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.decode(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"

        /** OWASP's 2023 floor for PBKDF2-SHA256. Slow on purpose. */
        const val ITERATIONS = 210_000
        const val KEY_BITS = 256
        const val SALT_BYTES = 16

        const val FREE_ATTEMPTS = 5
        const val BASE_PENALTY_MILLIS = 5_000L

        const val KEY_HASH = "pin_hash"
        const val KEY_SALT = "pin_salt"
        const val KEY_ENABLED = "lock_enabled"
        const val KEY_BIOMETRIC = "biometric_enabled"
        const val KEY_HIDE = "hide_in_recents"
        const val KEY_FAILED = "failed_attempts"
        const val KEY_LOCKED_UNTIL = "locked_until"
    }
}
