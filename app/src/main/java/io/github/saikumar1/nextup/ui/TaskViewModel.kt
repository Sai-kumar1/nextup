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
import io.github.saikumar1.nextup.data.Task
import io.github.saikumar1.nextup.data.TaskRepository
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

data class TaskUiState(
    val date: LocalDate,
    val today: LocalDate,
    val active: List<Task> = emptyList(),
    val completed: List<Task> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isToday: Boolean get() = date == today
    val isEmpty: Boolean get() = active.isEmpty() && completed.isEmpty()
    val mood: DayMood
        get() = when {
            isEmpty -> DayMood.Empty
            active.isEmpty() -> DayMood.AllDone
            else -> DayMood.InProgress
        }

    fun find(id: Long): Task? = active.find { it.id == id } ?: completed.find { it.id == id }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository,
    private val savedStateHandle: SavedStateHandle,
    private val clock: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val _today = MutableStateFlow(clock())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private val selectedDay: StateFlow<Long> =
        savedStateHandle.getStateFlow(KEY_SELECTED_DAY, _today.value.toEpochDay())

    private val selectedDate: LocalDate get() = LocalDate.ofEpochDay(selectedDay.value)

    val uiState: StateFlow<TaskUiState> = selectedDay
        .flatMapLatest { day -> repository.observeForDate(day).map { tasks -> day to tasks } }
        .combine(_today) { (day, tasks), today ->
            val (done, active) = tasks.partition { it.isDone }
            TaskUiState(
                date = LocalDate.ofEpochDay(day),
                today = today,
                active = active,
                completed = done,
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

    /** Call when the screen resumes: if the date rolled over while we sat on "today", follow it. */
    fun onResume() {
        val now = clock()
        val previous = _today.value
        if (now == previous) return
        _today.value = now
        if (selectedDate == previous) selectDate(now)
    }

    /** Per-day task counts for the inclusive range, keyed by date (days without tasks are absent). */
    fun dayCounts(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, DayCount>> =
        repository.observeDayCounts(from.toEpochDay(), to.toEpochDay())
            .map { counts -> counts.associateBy { LocalDate.ofEpochDay(it.dueDate) } }

    fun addTask(title: String, notes: String = "", date: LocalDate = selectedDate) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        viewModelScope.launch {
            repository.add(
                Task(title = cleanTitle, notes = notes.trim(), dueDate = date.toEpochDay()),
            )
        }
    }

    fun updateTask(task: Task, title: String, notes: String, date: LocalDate = task.date) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        viewModelScope.launch {
            repository.update(
                task.copy(title = cleanTitle, notes = notes.trim(), dueDate = date.toEpochDay()),
            )
        }
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

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NextUpApp
                TaskViewModel(app.container.taskRepository, createSavedStateHandle())
            }
        }
    }
}
