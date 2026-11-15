package io.github.saikumar1.nextup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.saikumar1.nextup.data.Task
import io.github.saikumar1.nextup.ui.components.AllDoneBanner
import io.github.saikumar1.nextup.ui.components.NothingPlanned
import io.github.saikumar1.nextup.ui.components.TaskDatePickerDialog
import io.github.saikumar1.nextup.ui.components.TaskDetailSheet
import io.github.saikumar1.nextup.ui.components.TaskEditorDialog
import io.github.saikumar1.nextup.ui.components.WeekStrip
import io.github.saikumar1.nextup.ui.components.friendlyDate
import io.github.saikumar1.nextup.ui.components.longDate
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Sentinel for "the editor is open for a brand-new task". */
private const val NEW_TASK_ID = 0L

/** Matches the site's --radius. */
private val CardShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    // null = closed; otherwise the id of the task shown in the view sheet.
    var viewingId by rememberSaveable { mutableStateOf<Long?>(null) }
    // null = closed, NEW_TASK_ID = adding, otherwise the id of the task being edited.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCompleted by rememberSaveable { mutableStateOf(true) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }

    fun showMessage(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    fun removeWithUndo(removed: List<Task>, message: String, remove: () -> Unit) {
        if (removed.isEmpty()) return
        remove()
        showMessage(message, "Undo") { viewModel.restore(removed) }
    }

    fun deleteTask(task: Task) =
        removeWithUndo(listOf(task), "Task deleted") { viewModel.delete(task) }

    /** After saving to a day other than the one on screen, offer to jump there. */
    fun announceIfElsewhere(date: LocalDate, verb: String) {
        if (date != state.date) {
            showMessage("$verb ${friendlyDate(date, state.today)}", "View") {
                viewModel.selectDate(date)
            }
        }
    }

    Scaffold(
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(friendlyDate(state.date, state.today))
                            Text(
                                longDate(state.date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        if (state.completed.isNotEmpty()) {
                            IconButton(onClick = {
                                val done = state.completed
                                removeWithUndo(done, "Cleared ${done.size} completed") {
                                    viewModel.clearCompleted()
                                }
                            }) {
                                Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear completed")
                            }
                        }
                        IconButton(onClick = { pickingDate = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick a date")
                        }
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = if (isDark) "Switch to light theme" else "Switch to dark theme",
                            )
                        }
                    },
                )
                WeekStrip(
                    selected = state.date,
                    today = state.today,
                    dayCounts = viewModel::dayCounts,
                    onSelect = viewModel::selectDate,
                    onToday = viewModel::goToToday,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                HorizontalDivider()
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingId = NEW_TASK_ID },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New task") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> Unit
            state.isEmpty -> NothingPlanned(
                date = state.date,
                today = state.today,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    // Leave room so the FAB never covers the last row.
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                if (state.mood == DayMood.AllDone) {
                    item(key = "all-done") {
                        AllDoneBanner(state.date, state.today, Modifier.animateItem())
                    }
                }
                items(state.active, key = { it.id }) { task ->
                    SwipeableTaskRow(
                        task = task,
                        onToggle = { viewModel.toggleDone(task) },
                        onClick = { viewingId = task.id },
                        onDelete = { deleteTask(task) },
                        modifier = Modifier.animateItem(),
                    )
                }
                if (state.completed.isNotEmpty()) {
                    item(key = "completed-header") {
                        CompletedHeader(
                            count = state.completed.size,
                            expanded = showCompleted,
                            onToggle = { showCompleted = !showCompleted },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    if (showCompleted) {
                        items(state.completed, key = { it.id }) { task ->
                            SwipeableTaskRow(
                                task = task,
                                onToggle = { viewModel.toggleDone(task) },
                                onClick = { viewingId = task.id },
                                onDelete = { deleteTask(task) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (pickingDate) {
        TaskDatePickerDialog(
            initial = state.date,
            onDismiss = { pickingDate = false },
            onConfirm = {
                viewModel.selectDate(it)
                pickingDate = false
            },
        )
    }

    // View mode.
    viewingId?.let { id ->
        val task = state.find(id)
        if (task != null) {
            TaskDetailSheet(
                task = task,
                today = state.today,
                onDismiss = { viewingId = null },
                onToggleDone = { viewModel.toggleDone(task) },
                onEdit = {
                    viewingId = null
                    editingId = task.id
                },
                onDelete = {
                    viewingId = null
                    deleteTask(task)
                },
            )
        } else if (!state.isLoading) {
            // The task was deleted or moved to another day.
            LaunchedEffect(id) { viewingId = null }
        }
    }

    // Add / edit mode.
    editingId?.let { id ->
        val existing = if (id == NEW_TASK_ID) null else state.find(id)
        if (id == NEW_TASK_ID || existing != null) {
            key(id) {
                TaskEditorDialog(
                    initial = existing,
                    defaultDate = state.date,
                    today = state.today,
                    onDismiss = { editingId = null },
                    onSave = { title, notes, date ->
                        if (existing == null) {
                            viewModel.addTask(title, notes, date)
                            announceIfElsewhere(date, "Added to")
                        } else {
                            viewModel.updateTask(existing, title, notes, date)
                            announceIfElsewhere(date, "Moved to")
                        }
                        editingId = null
                    },
                )
            }
        } else if (!state.isLoading) {
            LaunchedEffect(id) { editingId = null }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskRow(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    var deleted by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && !deleted) {
                deleted = true
                currentOnDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(CardShape),
        backgroundContent = {
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        TaskRow(task = task, onToggle = onToggle, onClick = onClick)
    }
}

@Composable
private fun TaskRow(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    val contentAlpha = if (task.isDone) 0.6f else 1f
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        // The white "sheet" in light, a raised gray in dark.
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        leadingContent = {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
        },
        headlineContent = {
            Text(
                text = task.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
        },
        supportingContent = if (task.notes.isNotBlank()) {
            {
                Text(
                    text = task.notes,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }
        } else {
            null
        },
        trailingContent = if (task.notes.isNotBlank()) {
            {
                Icon(
                    Icons.AutoMirrored.Outlined.Notes,
                    contentDescription = "Has notes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun CompletedHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Completed ($count)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "Hide completed" else "Show completed",
        )
    }
}
