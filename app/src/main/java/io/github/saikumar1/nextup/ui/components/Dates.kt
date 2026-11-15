package io.github.saikumar1.nextup.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "Today", "Tomorrow", "Yesterday", or a short date like "Mon, 17 Nov" (with year if not this year). */
fun friendlyDate(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    today.minusDays(1) -> "Yesterday"
    else -> date.format(
        DateTimeFormatter.ofPattern(
            if (date.year == today.year) "EEE, d MMM" else "EEE, d MMM yyyy",
            Locale.getDefault(),
        ),
    )
}

/**
 * Screen title: relative wording people think in, never the full date (the week strip
 * already shows exactly which day it is).
 * Today / Tomorrow / Yesterday, the weekday within a week ("Wednesday", "Last Friday"),
 * otherwise a short date ("Wed, 24 Dec").
 */
fun headerTitle(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return when (days) {
        0L, 1L, -1L -> friendlyDate(date, today)
        in 2L..6L -> weekday
        in -6L..-2L -> "Last $weekday"
        else -> friendlyDate(date, today)
    }
}

/**
 * Status line under the title: how the day is going, plus how far away it is when that
 * isn't already obvious from the title. E.g. "3 of 5 done", "All done ✓",
 * "In 9 days · 2 tasks", "Nothing planned".
 */
fun headerSubtitle(date: LocalDate, today: LocalDate, total: Int, done: Int): String {
    val days = ChronoUnit.DAYS.between(today, date)
    val distance = when {
        days > 1 -> "In $days days"
        days < -1 -> "${-days} days ago"
        else -> null
    }
    val status = when {
        total == 0 -> "Nothing planned"
        done >= total -> "All done ✓"
        days > 0 && done == 0 -> if (total == 1) "1 task" else "$total tasks"
        else -> "$done of $total done"
    }
    return listOfNotNull(distance, status).joinToString(" · ")
}

/** Full date like "Sunday, 15 November 2026". */
fun longDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))

// The Material date picker works in UTC-midnight millis.
private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.fromPickerMillis(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.toPickerMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(it.fromPickerMillis()) } },
                enabled = state.selectedDateMillis != null,
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}
