package io.github.saikumar1.nextup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.saikumar1.nextup.NextUpApp
import io.github.saikumar1.nextup.data.Task
import io.github.saikumar1.nextup.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskUiState(
    val active: List<Task> = emptyList(),
    val completed: List<Task> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = active.isEmpty() && completed.isEmpty()

    fun find(id: Long): Task? = active.find { it.id == id } ?: completed.find { it.id == id }
}

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val uiState: StateFlow<TaskUiState> = repository.observeTasks()
        .map { tasks ->
            val (done, active) = tasks.partition { it.isDone }
            TaskUiState(active = active, completed = done, isLoading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskUiState())

    fun addTask(title: String, notes: String = "") {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        viewModelScope.launch { repository.add(Task(title = cleanTitle, notes = notes.trim())) }
    }

    fun updateTask(task: Task, title: String, notes: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        viewModelScope.launch {
            repository.update(task.copy(title = cleanTitle, notes = notes.trim()))
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

    fun clearCompleted() {
        viewModelScope.launch { repository.clearCompleted() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NextUpApp
                TaskViewModel(app.container.taskRepository)
            }
        }
    }
}
