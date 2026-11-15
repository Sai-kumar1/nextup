package io.github.saikumar1.nextup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.saikumar1.nextup.data.Priority
import io.github.saikumar1.nextup.data.Task
import io.github.saikumar1.nextup.ui.components.AllDoneBanner
import io.github.saikumar1.nextup.ui.components.NothingPlanned
import io.github.saikumar1.nextup.ui.components.PriorityChangeDialog
import io.github.saikumar1.nextup.ui.components.PriorityIcon
import io.github.saikumar1.nextup.ui.components.TaskDatePickerDialog
import io.github.saikumar1.nextup.ui.components.TaskDetailSheet
import io.github.saikumar1.nextup.ui.components.TaskEditorDialog
import io.github.saikumar1.nextup.ui.components.WeekStrip
import io.github.saikumar1.nextup.ui.components.color
import io.github.saikumar1.nextup.ui.components.friendlyDate
import io.github.saikumar1.nextup.ui.components.groupLabel
import io.github.saikumar1.nextup.ui.components.label
import io.github.saikumar1.nextup.ui.components.longDate
import io.github.saikumar1.nextup.ui.reorder.DropResult
import io.github.saikumar1.nextup.ui.reorder.ListRow
import io.github.saikumar1.nextup.ui.reorder.buildRows
import io.github.saikumar1.nextup.ui.reorder.groupSize
import io.github.saikumar1.nextup.ui.reorder.moveRow
import io.github.saikumar1.nextup.ui.reorder.resolveDrop
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDate

/** Sentinel for "the editor is open for a brand-new task". */
private const val NEW_TASK_ID = 0L

/** Matches the site's --radius. */
private val CardShape = RoundedCornerShape(12.dp)

/** A cross-group drop waiting for the user to confirm the priority change. */
private data class PendingMove(val task: Task, val drop: DropResult)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    confirmPriorityChange: Boolean,
    onConfirmPriorityChangeSet: (Boolean) -> Unit,
    viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    // null = closed; otherwise the id of the task shown in the view sheet.
    var viewingId by rememberSaveable { mutableStateOf<Long?>(null) }
    // null = closed, NEW_TASK_ID = adding, otherwise the id of the task being edited.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCompleted by rememberSaveable { mutableStateOf(true) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    // ---- Drag-to-reorder state ----
    val baseRows = remember(state.groups) { buildRows(state.groups) }
    // While dragging (and until the database catches up) the list shows this local copy.
    var draggingRows by remember { mutableStateOf<List<ListRow>?>(null) }
    var awaitingCommit by remember { mutableStateOf(false) }
    var pendingMove by remember { mutableStateOf<PendingMove?>(null) }
    val rows = draggingRows ?: baseRows

    LaunchedEffect(baseRows) {
        if (awaitingCommit) {
            awaitingCommit = false
            draggingRows = null
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val current = draggingRows ?: return@rememberReorderableLazyListState
        moveRow(current, from.key, to.key)?.let {
            draggingRows = it
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun commitMove(task: Task, drop: DropResult) {
        awaitingCommit = true
        viewModel.moveTask(task.id, drop.priority, drop.groupOrder)
    }

    fun finishDrag(task: Task) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val moved = draggingRows ?: return
        val drop = resolveDrop(moved, task.id)
        val original = resolveDrop(baseRows, task.id)
        when {
            drop == null || drop == original -> draggingRows = null
            drop.priority == task.priority -> commitMove(task, drop)
            confirmPriorityChange -> pendingMove = PendingMove(task, drop)
            else -> commitMove(task, drop)
        }
    }

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
                        IconButton(onClick = { pickingDate = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick a date")
                        }
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = if (isDark) "Switch to light theme" else "Switch to dark theme",
                            )
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Clear completed") },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                                    enabled = state.allCompleted.isNotEmpty(),
                                    onClick = {
                                        menuOpen = false
                                        val done = state.allCompleted
                                        removeWithUndo(done, "Cleared ${done.size} completed") {
                                            viewModel.clearCompleted()
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Ask before changing priority") },
                                    leadingIcon = {
                                        Checkbox(checked = confirmPriorityChange, onCheckedChange = null)
                                    },
                                    onClick = { onConfirmPriorityChangeSet(!confirmPriorityChange) },
                                )
                            }
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
                if (!state.isEmpty) {
                    FilterChips(
                        selected = state.filter,
                        onSelect = viewModel::setFilter,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
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
                state = lazyListState,
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
                if (state.filteredOut) {
                    item(key = "filtered-out") {
                        FilteredOut(
                            filter = state.filter,
                            onShowAll = { viewModel.setFilter(PriorityFilter.ALL) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                // Groups are only shown while the day (or filter) has unfinished tasks.
                if (state.groups.any { it.tasks.isNotEmpty() }) {
                    itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                        ReorderableItem(reorderState, key = row.key) { isDragging ->
                            when (row) {
                                is ListRow.Header -> GroupHeader(
                                    priority = row.priority,
                                    count = groupSize(rows, index),
                                    showDropHint = state.filter == PriorityFilter.ALL,
                                )
                                is ListRow.Item -> {
                                    val task = row.task
                                    SwipeableTaskRow(
                                        task = task,
                                        isDragging = isDragging,
                                        onToggle = { viewModel.toggleDone(task) },
                                        onClick = { viewingId = task.id },
                                        onDelete = { deleteTask(task) },
                                        dragHandle = {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .draggableHandle(
                                                        onDragStarted = {
                                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            if (draggingRows == null) draggingRows = baseRows
                                                        },
                                                        onDragStopped = { finishDrag(task) },
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    Icons.Filled.DragIndicator,
                                                    contentDescription = "Drag to reorder",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
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
                                isDragging = false,
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

    pendingMove?.let { pending ->
        PriorityChangeDialog(
            task = pending.task,
            to = pending.drop.priority,
            onConfirm = { dontAskAgain ->
                if (dontAskAgain) onConfirmPriorityChangeSet(false)
                pendingMove = null
                commitMove(pending.task, pending.drop)
            },
            onDismiss = {
                // Snap the task back to where it was.
                pendingMove = null
                draggingRows = null
            },
        )
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
                    onSave = { title, notes, date, priority ->
                        if (existing == null) {
                            viewModel.addTask(title, notes, date, priority)
                            announceIfElsewhere(date, "Added to")
                        } else {
                            viewModel.updateTask(existing, title, notes, date, priority)
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

@Composable
private fun FilterChips(
    selected: PriorityFilter,
    onSelect: (PriorityFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PriorityFilter.entries.forEach { filter ->
            val priority = filter.priority
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(priority?.label ?: "All") },
                leadingIcon = priority?.let { { PriorityIcon(it, contentDescription = null) } },
            )
        }
    }
}

@Composable
private fun GroupHeader(priority: Priority, count: Int, showDropHint: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (priority != Priority.NONE) {
            PriorityIcon(priority, contentDescription = null)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = priority.groupLabel,
            style = MaterialTheme.typography.titleSmall,
            color = if (priority == Priority.NONE) MaterialTheme.colorScheme.onSurfaceVariant else priority.color(),
        )
        Text(
            text = "  $count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (count == 0 && showDropHint) {
            Text(
                text = "Drag a task here",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FilteredOut(filter: PriorityFilter, onShowAll: () -> Unit, modifier: Modifier = Modifier) {
    val priority = filter.priority ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PriorityIcon(priority, size = 40.dp, contentDescription = null)
        Spacer(Modifier.padding(top = 12.dp))
        Text(
            "No ${priority.label.lowercase()}-priority tasks for this day",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onShowAll) { Text("Show all tasks") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskRow(
    task: Task,
    isDragging: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: (@Composable () -> Unit)? = null,
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
    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        shape = CardShape,
        shadowElevation = if (isDragging) 8.dp else 0.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        SwipeToDismissBox(
            state = dismissState,
            // No swiping away a row that is being dragged.
            gesturesEnabled = !isDragging,
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
            TaskRow(task = task, onToggle = onToggle, onClick = onClick, dragHandle = dragHandle)
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    dragHandle: (@Composable () -> Unit)?,
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.priority != Priority.NONE) {
                    PriorityIcon(task.priority, size = 16.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = task.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                )
            }
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
        trailingContent = dragHandle,
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
