package io.github.saikumar1.nextup.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.saikumar1.nextup.data.Task
import java.time.LocalDate

/** Add (initial == null) or edit a task: title, notes and the day it's planned for. */
@Composable
fun TaskEditorDialog(
    initial: Task?,
    defaultDate: LocalDate,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String, date: LocalDate) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(initial?.notes.orEmpty()) }
    var day by rememberSaveable {
        mutableLongStateOf(initial?.dueDate ?: defaultDate.toEpochDay())
    }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(day)
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New task" else "Edit task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("Details, links…") },
                    minLines = 3,
                    maxLines = 8,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    AssistChip(
                        onClick = { pickingDate = true },
                        label = { Text(friendlyDate(date, today)) },
                        leadingIcon = { Icon(Icons.Outlined.Event, contentDescription = "Planned for") },
                    )
                }
                LaunchedEffect(Unit) {
                    if (initial == null) focusRequester.requestFocus()
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, notes, date) },
                enabled = title.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
    )

    if (pickingDate) {
        TaskDatePickerDialog(
            initial = date,
            onDismiss = { pickingDate = false },
            onConfirm = {
                day = it.toEpochDay()
                pickingDate = false
            },
        )
    }
}
