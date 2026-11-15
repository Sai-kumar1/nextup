package io.github.saikumar1.nextup.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.saikumar1.nextup.NextUpApp
import io.github.saikumar1.nextup.data.DayCount
import io.github.saikumar1.nextup.data.Priority
import io.github.saikumar1.nextup.data.Task
import io.github.saikumar1.nextup.data.TaskRepository
import io.github.saikumar1.nextup.ui.reorder.TaskGroup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class DayMood { Empty, AllDone, InProgress }

/** Single-choice priority filter. */
enum class PriorityFilter(val priority: Priority?) {
    ALL(null),
    HIGH(Priority.HIGH),
    MEDIUM(Priority.MEDIUM),
    LOW(Priority.LOW),
    ;

    fun matches(task: Task): Boolean = priority == null || task.priority == priority
}

data class TaskUiState(
    val date: LocalDate,
    val today: LocalDate,
    /** All of the day's unfinished tasks, unfiltered, in group then manual order. */
    val active: List<Task> = emptyList(),
    /** All of the day's finished tasks, unfiltered. */
    val allCompleted: List<Task> = emptyList(),
    val filter: PriorityFilter = PriorityFilter.ALL,
    val isLoading: Boolean = true,
) {
    val isToday: Boolean get() = date == today
    val isEmpty: Boolean get() = active.isEmpty() && allCompleted.isEmpty()

    /** Based on the whole day, regardless of the filter. */
    val mood: DayMood
        get() = when {
            isEmpty -> DayMood.Empty
            active.isEmpty() -> DayMood.AllDone
            else -> DayMood.InProgress
        }

    /** Active tasks grouped by priority: all four groups for ALL, else just the filtered one. */
    val groups: List<TaskGroup>
        get() {
            val priorities = filter.priority?.let { listOf(it) } ?: Priority.displayOrder
            return priorities.map { p -> TaskGroup(p, active.filter { it.priority == p }) }
        }

    val completed: List<Task> get() = allCompleted.filter(filter::matches)

    /** True when the filter hides every task of the day. */
    val filteredOut: Boolean
        get() = !isEmpty && groups.all { it.tasks.isEmpty() } && completed.isEmpty()

    fun find(id: Long): Task? = active.find { it.id == id } ?: allCompleted.find { it.id == id }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository,
    private val savedStateHandle: SavedStateHandle,
    private val clock: () -> LocalDate = LocalDate::now,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _today = MutableStateFlow(clock())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private val selectedDay: StateFlow<Long> =
        savedStateHandle.getStateFlow(KEY_SELECTED_DAY, _today.value.toEpochDay())

    private val filterName: StateFlow<String> =
        savedStateHandle.getStateFlow(KEY_FILTER, PriorityFilter.ALL.name)

    private val selectedDate: LocalDate get() = LocalDate.ofEpochDay(selectedDay.value)

    val uiState: StateFlow<TaskUiState> = selectedDay
        .flatMapLatest { day -> repository.observeForDate(day).map { tasks -> day to tasks } }
        .combine(_today) { dayAndTasks, today -> dayAndTasks to today }
        .combine(filterName) { (dayAndTasks, today), filter ->
            val (day, tasks) = dayAndTasks
            val (done, active) = tasks.partition { it.isDone }
            TaskUiState(
                date = LocalDate.ofEpochDay(day),
                today = today,
                active = Priority.displayOrder.flatMap { p -> active.filter { it.priority == p } },
                allCompleted = done,
                filter = PriorityFilter.valueOf(filter),
                isLoading = false,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TaskUiState(date = selectedDate, today = _today.value),
        )

    fun selectDate(date: LocalDate) {
        savedStateHandle[KEY_SELECTED_DAY] = date.toEpochDay()
    }

    fun goToToday() = selectDate(_today.value)

    /** Selecting the active filter again goes back to ALL. */
    fun setFilter(filter: PriorityFilter) {
        val current = PriorityFilter.valueOf(filterName.value)
        savedStateHandle[KEY_FILTER] =
            (if (filter == current) PriorityFilter.ALL else filter).name
    }

    /** Call when the screen resumes: if the date rolled over while we sat on "today", follow it. */
    fun onResume() {
        val current = clock()
        val previous = _today.value
        if (current == previous) return
        _today.value = current
        if (selectedDate == previous) selectDate(current)
    }

    /** Per-day task counts for the inclusive range, keyed by date (days without tasks are absent). */
    fun dayCounts(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, DayCount>> =
        repository.observeDayCounts(from.toEpochDay(), to.toEpochDay())
            .map { counts -> counts.associateBy { LocalDate.ofEpochDay(it.dueDate) } }

    fun addTask(
        title: String,
        notes: String = "",
        date: LocalDate = selectedDate,
        priority: Priority = Priority.NONE,
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val time = now()
        viewModelScope.launch {
            repository.add(
                Task(
                    title = cleanTitle,
                    notes = notes.trim(),
                    dueDate = date.toEpochDay(),
                    priority = priority,
                    createdAt = time,
                    position = time,
                ),
            )
        }
    }

    fun updateTask(
        task: Task,
        title: String,
        notes: String,
        date: LocalDate = task.date,
        priority: Priority = task.priority,
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        // A task moved to another day or group goes to the bottom of it.
        val movedGroup = date.toEpochDay() != task.dueDate || priority != task.priority
        viewModelScope.launch {
            repository.update(
                task.copy(
                    title = cleanTitle,
                    notes = notes.trim(),
                    dueDate = date.toEpochDay(),
                    priority = priority,
                    position = if (movedGroup) now() else task.position,
                ),
            )
        }
    }

    /**
     * Applies a drag: [taskId] now has [priority], and that group's tasks are in [groupOrder].
     * Other groups keep their relative order.
     */
    fun moveTask(taskId: Long, priority: Priority, groupOrder: List<Long>) {
        val byId = uiState.value.active.associateBy { it.id }
        val updates = groupOrder.mapIndexedNotNull { index, id ->
            val task = byId[id] ?: return@mapIndexedNotNull null
            val newPriority = if (id == taskId) priority else task.priority
            if (task.position == index.toLong() && task.priority == newPriority) {
                null
            } else {
                task.copy(position = index.toLong(), priority = newPriority)
            }
        }
        if (updates.isEmpty()) return
        viewModelScope.launch { repository.updateAll(updates) }
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch { repository.update(task.copy(isDone = !task.isDone)) }
    }

    fun delete(task: Task) {
        viewModelScope.launch { repository.delete(task) }
    }

    /** Re-inserts previously deleted tasks with their original ids (used by Undo). */
    fun restore(tasks: List<Task>) {
        viewModelScope.launch { tasks.forEach { repository.add(it) } }
    }

    /** Removes the completed tasks of the selected day only. */
    fun clearCompleted() {
        val day = selectedDay.value
        viewModelScope.launch { repository.clearCompleted(day) }
    }

    companion object {
        private const val KEY_SELECTED_DAY = "selected_day"
        private const val KEY_FILTER = "priority_filter"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NextUpApp
                TaskViewModel(app.container.taskRepository, createSavedStateHandle())
            }
        }
    }
}
