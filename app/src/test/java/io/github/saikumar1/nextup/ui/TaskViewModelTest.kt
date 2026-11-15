package io.github.saikumar1.nextup.ui

import io.github.saikumar1.nextup.data.Task
import io.github.saikumar1.nextup.data.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeTaskRepository : TaskRepository {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L

    override fun observeTasks(): Flow<List<Task>> = tasks

    override suspend fun add(task: Task): Long {
        val id = if (task.id == 0L) nextId++ else task.id
        tasks.update { list -> list.filterNot { it.id == id } + task.copy(id = id) }
        return id
    }

    override suspend fun update(task: Task) =
        tasks.update { list -> list.map { if (it.id == task.id) task else it } }

    override suspend fun delete(task: Task) =
        tasks.update { list -> list.filterNot { it.id == task.id } }

    override suspend fun clearCompleted() =
        tasks.update { list -> list.filterNot { it.isDone } }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: TaskViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = TaskViewModel(FakeTaskRepository())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** stateIn(WhileSubscribed) only updates while someone collects. */
    private fun TestScope.subscribe() {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun addTask_trimsAndIgnoresBlankTitles() = runTest(dispatcher) {
        subscribe()
        viewModel.addTask("  Buy milk  ", " 2 litres ")
        viewModel.addTask("   ")

        assertEquals(1, state.active.size)
        assertEquals("Buy milk", state.active[0].title)
        assertEquals("2 litres", state.active[0].notes)
    }

    @Test
    fun toggleDone_movesTaskBetweenSections() = runTest(dispatcher) {
        subscribe()
        viewModel.addTask("Write report")
        viewModel.toggleDone(state.active.single())

        assertTrue(state.active.isEmpty())
        assertEquals("Write report", state.completed.single().title)

        viewModel.toggleDone(state.completed.single())
        assertEquals(1, state.active.size)
    }

    @Test
    fun deleteThenRestore_bringsTaskBack() = runTest(dispatcher) {
        subscribe()
        viewModel.addTask("Call mom")
        val task = state.active.single()

        viewModel.delete(task)
        assertTrue(state.isEmpty)

        viewModel.restore(listOf(task))
        assertEquals(task, state.active.single())
    }

    @Test
    fun clearCompleted_keepsActiveTasks() = runTest(dispatcher) {
        subscribe()
        viewModel.addTask("Keep me")
        viewModel.addTask("Done one")
        viewModel.toggleDone(state.active.first { it.title == "Done one" })

        viewModel.clearCompleted()

        assertEquals(listOf("Keep me"), state.active.map { it.title })
        assertTrue(state.completed.isEmpty())
    }

    @Test
    fun updateTask_changesTitleAndNotes() = runTest(dispatcher) {
        subscribe()
        viewModel.addTask("Old")
        viewModel.updateTask(state.active.single(), " New ", " details ")

        assertEquals("New", state.active.single().title)
        assertEquals("details", state.active.single().notes)
    }
}
