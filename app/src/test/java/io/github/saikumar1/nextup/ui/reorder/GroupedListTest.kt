package io.github.saikumar1.nextup.ui.reorder

import io.github.saikumar1.nextup.data.Priority
import io.github.saikumar1.nextup.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupedListTest {
    private fun task(id: Long, priority: Priority) = Task(id = id, title = "t$id", priority = priority)

    private val high1 = task(1, Priority.HIGH)
    private val high2 = task(2, Priority.HIGH)
    private val none3 = task(3, Priority.NONE)

    private val rows = buildRows(
        listOf(
            TaskGroup(Priority.HIGH, listOf(high1, high2)),
            TaskGroup(Priority.MEDIUM, emptyList()),
            TaskGroup(Priority.LOW, emptyList()),
            TaskGroup(Priority.NONE, listOf(none3)),
        ),
    )

    private fun keys(list: List<ListRow>) = list.map { it.key }

    @Test
    fun buildRows_includesHeadersForEmptyGroups() {
        assertEquals(
            listOf("h-HIGH", 1L, 2L, "h-MEDIUM", "h-LOW", "h-NONE", 3L),
            keys(rows),
        )
        assertEquals(2, groupSize(rows, 0))
        assertEquals(0, groupSize(rows, 3))
    }

    @Test
    fun movingWithinAGroup_reordersIt() {
        val moved = moveRow(rows, 2L, 1L)!!
        assertEquals(DropResult(Priority.HIGH, listOf(2L, 1L)), resolveDrop(moved, 2L))
    }

    @Test
    fun movingPastAHeader_changesTheGroup() {
        // Drag task 3 (no priority) up past the LOW header into MEDIUM's empty group.
        val step1 = moveRow(rows, 3L, "h-NONE")!!
        assertEquals(DropResult(Priority.LOW, listOf(3L)), resolveDrop(step1, 3L))
        val step2 = moveRow(step1, 3L, "h-LOW")!!
        assertEquals(DropResult(Priority.MEDIUM, listOf(3L)), resolveDrop(step2, 3L))
    }

    @Test
    fun droppingAtTheEndOfAnotherGroup() {
        // Task 1 moves down to just above the MEDIUM header: still HIGH, now last.
        val moved = moveRow(rows, 1L, 2L)!!
        assertEquals(DropResult(Priority.HIGH, listOf(2L, 1L)), resolveDrop(moved, 1L))
        // One more step down crosses into MEDIUM.
        val crossed = moveRow(moved, 1L, "h-MEDIUM")!!
        assertEquals(DropResult(Priority.MEDIUM, listOf(1L)), resolveDrop(crossed, 1L))
        assertEquals(DropResult(Priority.HIGH, listOf(2L)), resolveDrop(crossed, 2L))
    }

    @Test
    fun cannotMoveAboveTheFirstHeader() {
        assertNull(moveRow(rows, 1L, "h-HIGH"))
    }

    @Test
    fun headersCannotBeDragged() {
        assertNull(moveRow(rows, "h-MEDIUM", 1L))
    }

    @Test
    fun unresolvedTaskReturnsNull() {
        assertNull(resolveDrop(rows, 99L))
    }
}
