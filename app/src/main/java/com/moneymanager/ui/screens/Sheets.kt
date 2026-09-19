package com.moneymanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.moneymanager.data.short
import com.moneymanager.data.symbolOf
import com.moneymanager.ui.MoneyType
import com.moneymanager.ui.Plate

/*
 * Small edits get a sheet, not a screen.
 *
 * Setting a budget limit or naming an account is one or two fields and a decision the user has
 * already made before they tapped. A whole pushed screen for that is ceremony, and Material's
 * bottom sheet is the platform's own answer. The one place that keeps a full screen is logging a
 * transaction, because that is the path that has to survive a hurry.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneySheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            Modifier
                .padding(horizontal = Gutter)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}

/**
 * Money in, typed.
 *
 * Digits only, held as minor units so nothing is ever parsed out of a formatted string and back
 * again. The system numeric keyboard is right here: this is a considered decision the user is
 * making at a table, not the ten-second logging path that earned its own keypad.
 */
@Composable
fun AmountInput(
    digits: String,
    onDigits: (String) -> Unit,
    label: String,
    currency: String = "USD",
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        Plate(padding = 16.dp) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(symbolOf(currency), style = MoneyType.medium, color = scheme.onSurfaceVariant)
                Box(Modifier.padding(start = 6.dp)) {
                    if (digits.isEmpty()) {
                        Text("0.00", style = MoneyType.medium, color = scheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    BasicTextField(
                        value = digits,
                        onValueChange = { new -> onDigits(new.filter { it.isDigit() }.take(11)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MoneyType.medium.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(scheme.primary),
                        visualTransformation = MoneyDigits,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** A plain single-line text field in the app's own plate. */
@Composable
fun TextInput(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        Plate(padding = 16.dp) {
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValue,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                    cursorBrush = SolidColor(scheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Shows a digit string as money while it is being typed, so the user reads the number they are
 * entering rather than the raw digits.
 *
 * The cursor is pinned to the end in both directions. Amount entry is append-only -- there is no
 * meaningful place to put a caret inside a formatted figure -- and a naive identity mapping would
 * crash the moment the formatted text got longer than what was typed.
 */
private object MoneyDigits : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = asMoney(text.text)
        return TransformedText(
            AnnotatedString(shown),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = shown.length
                override fun transformedToOriginal(offset: Int) = text.text.length
            },
        )
    }
}

/**
 * A date, chosen from the platform's own calendar.
 *
 * Material's picker works in UTC milliseconds and this app works in epoch days, so the conversion
 * happens here once rather than being repeated at every call site and getting it wrong somewhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: java.time.LocalDate,
    onDate: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        Plate(padding = 16.dp, onClick = { open = true }) {
            Text(
                "${date.dayOfMonth} ${date.month.short()} ${date.year}",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
            )
        }
    }

    if (open) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * DAY_MILLIS)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onDate(java.time.LocalDate.ofEpochDay(it / DAY_MILLIS)) }
                    open = false
                }) { Text("Choose") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) { DatePicker(state = picker) }
    }
}

private const val DAY_MILLIS = 86_400_000L

/** Minor-unit digit string to "1,284.12", without going through a Double on the way. */
fun asMoney(digits: String): String {
    val padded = digits.ifEmpty { "0" }.padStart(3, '0')
    val whole = padded.dropLast(2).trimStart('0').ifEmpty { "0" }
    val grouped = buildString {
        whole.forEachIndexed { i, c ->
            if (i > 0 && (whole.length - i) % 3 == 0) append(',')
            append(c)
        }
    }
    return "$grouped.${padded.takeLast(2)}"
}

fun digitsToMinor(digits: String): Long = digits.filter { it.isDigit() }.toLongOrNull() ?: 0L

fun minorToDigits(minor: Long): String = if (minor == 0L) "" else minor.toString()
