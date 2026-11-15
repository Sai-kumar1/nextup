package io.github.saikumar1.nextup.ui

import androidx.lifecycle.SavedStateHandle
import io.github.saikumar1.nextup.data.DayCount
import io.github.saikumar1.nextup.data.Task
import io.github.saikumar1.nextup.data.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

private class FakeTaskRepository : TaskRepository {
    val tasks = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L

    override fun observeForDate(day: Long): Flow<List<Task>> =
        tasks.map { list -> list.filter { it.dueDate == day } }

    override fun observeDayCounts(from: Long, to: Long): Flow<List<DayCount>> =
        tasks.map { list ->
            list.filter { it.dueDate in from..to }
                .groupBy { it.dueDate }
                .map { (day, dayTasks) -> DayCount(day, dayTasks.size, dayTasks.count { it.isDone }) }
        }

    override suspend fun add(task: Task): Long {
        val id = if (task.id == 0L) nextId++ else task.id
        // Distinct, increasing createdAt keeps ordering deterministic.
        tasks.update { list -> list.filterNot { it.id == id } + task.copy(id = id, createdAt = id) }
        return id
    }

    override suspend fun update(task: Task) =
        tasks.update { list -> list.map { if (it.id == task.id) task else it } }

    override suspend fun delete(task: Task) =
        tasks.update { list -> list.filterNot { it.id == task.id } }

    override suspend fun clearCompleted(day: Long) =
        tasks.update { list -> list.filterNot { it.isDone && it.dueDate == day } }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 11, 15)
    private val tomorrow = today.plusDays(1)
    private var clockDate = today
    private lateinit var repository: FakeTaskRepository
    private lateinit var viewModel: TaskViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeTaskRepository()
        viewModel = TaskViewModel(repository, SavedStateHandle(), clock = { clockDate })
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** stateIn(WhileSubscribed) only updates while someone collects. */
    private fun TestScope.subscribe() {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun startsOnToday() = runTest(dispatcher) {
        subscribe()
        assertEquals(today, state.date)
        assertTrue(state.isToday)
    }

    @Test
    fun addTask_usesSelectedDate_andTrims() = runTest(dispatcher) {
        subscribe()
        viewModel.addTask("  Buy milk  ", " 2 litres ")
        viewModel.addTask("   ")

        assertEquals(1, state.active.size)
        assertEquals("Buy milk", state.active[0].title)
        assertEquals("2 litres", state.active[0].notes)
        assertEquals(today, state.active[0].date)
    }

    @Test
    fun onlySelectedDatesTasksAreShown() = runTest(dispatcher) {
        subscribe()
        viewModel.addTask("Today task")
        viewModel.addTask("Tomorrow task", date = tomorrow)

        assertEquals(listOf("Today task"), state.active.map { it.title })

        viewModel.selectDate(tomorrow)
        assertEquals(tomorrow, state.date)
        assertFalse(state.isToday)
        assertEquals(listOf("Tomorrow task"), state.active.map { it.title })

        viewModel.goToToday()
        assertEquals(listOf("Today task"), state.active.map { it.title })
    }

    @Test
    fun updateTask_canMoveTaskToAnotherDay() = runTest(dispatcher) {
        subscribe()
        viewModel.addTask("Old")
        viewModel.updateTask(state.active.single(), " New ", " details ", tomorrow)

        assertTrue(state.isEmpty)
        viewModel.selectDate(tomorrow)
        assertEquals("New", state.active.single().title)
        assertEquals("details", state.active.single().notes)
    }

    @Test
    fun mood_goesFromEmptyToInProgressToAllDone() = runTest(dispatcher) {
        subscribe()
        assertEquals(DayMood.Empty, state.mood)

        viewModel.addTask("One")
        viewModel.addTask("Two")
        assertEquals(DayMood.InProgress, state.mood)

        viewModel.toggleDone(state.active[0])
        assertEquals(DayMood.InProgress, state.mood)

        viewModel.toggleDone(state.active.single())
        assertEquals(DayMood.AllDone, state.mood)
        assertEquals(2, state.completed.size)
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
    fun clearCompleted_onlyAffectsSelectedDay() = runTest(dispatcher) {
        subscribe()
        viewModel.addTask("Done today")
        viewModel.addTask("Keep me")
        viewModel.toggleDone(state.active.first { it.title == "Done today" })
        viewModel.addTask("Done tomorrow", date = tomorrow)
        viewModel.selectDate(tomorrow)
        viewModel.toggleDone(state.active.single())
        viewModel.goToToday()

        viewModel.clearCompleted()

        assertEquals(listOf("Keep me"), state.active.map { it.title })
        assertTrue(state.completed.isEmpty())
        viewModel.selectDate(tomorrow)
        assertEquals(listOf("Done tomorrow"), state.completed.map { it.title })
    }

    @Test
    fun onResume_followsMidnightOnlyWhenViewingToday() = runTest(dispatcher) {
        subscribe()
        clockDate = tomorrow
        viewModel.onResume()
        assertEquals(tomorrow, state.date)
        assertTrue(state.isToday)

        viewModel.selectDate(today.minusDays(3))
        clockDate = tomorrow.plusDays(1)
        viewModel.onResume()
        assertEquals(today.minusDays(3), state.date)
        assertEquals(tomorrow.plusDays(1), state.today)
    }

    @Test
    fun dayCounts_reportTotalsPerDay() = runTest(dispatcher) {
        viewModel.addTask("A")
        viewModel.addTask("B")
        viewModel.addTask("C", date = tomorrow)
        repository.tasks.update { list -> list.map { if (it.title == "A") it.copy(isDone = true) else it } }

        val counts = viewModel.dayCounts(today, today.plusDays(6)).first()

        assertEquals(2, counts.getValue(today).total)
        assertEquals(1, counts.getValue(today).done)
        assertEquals(1, counts.getValue(tomorrow).total)
        assertFalse(counts.containsKey(today.plusDays(2)))
    }
}
