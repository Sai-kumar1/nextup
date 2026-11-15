package io.github.saikumar1.nextup.ui.reorder

import io.github.saikumar1.nextup.data.Priority
import io.github.saikumar1.nextup.data.Task

/** Active tasks of one priority, in manual order. */
data class TaskGroup(val priority: Priority, val tasks: List<Task>)

/** A row of the reorderable list: a priority group header, or a task under it. */
sealed interface ListRow {
    val key: Any

    data class Header(val priority: Priority) : ListRow {
        override val key: Any get() = headerKey(priority)
    }

    data class Item(val task: Task) : ListRow {
        override val key: Any get() = task.id
    }
}

fun headerKey(priority: Priority): String = "h-${priority.name}"

/** Where a dragged task ended up: its (possibly new) priority and its group's new order. */
data class DropResult(val priority: Priority, val groupOrder: List<Long>)

/** One header per group (empty groups included, so tasks can be dropped into them), then its tasks. */
fun buildRows(groups: List<TaskGroup>): List<ListRow> = groups.flatMap { group ->
    listOf(ListRow.Header(group.priority)) + group.tasks.map { ListRow.Item(it) }
}

/**
 * Moves the row with [fromKey] to where [toKey] is. Returns null when the move isn't
 * allowed: unknown keys, dragging a header, or anything that would leave a task above the
 * first header (every task must sit under a group).
 */
fun moveRow(rows: List<ListRow>, fromKey: Any, toKey: Any): List<ListRow>? {
    val from = rows.indexOfFirst { it.key == fromKey }
    val to = rows.indexOfFirst { it.key == toKey }
    if (from < 0 || to < 0 || from == to) return null
    if (rows[from] !is ListRow.Item) return null
    val moved = rows.toMutableList().apply { add(to, removeAt(from)) }
    return if (moved.first() is ListRow.Header) moved else null
}

/** Number of tasks directly under the header at [headerIndex] (live while dragging). */
fun groupSize(rows: List<ListRow>, headerIndex: Int): Int =
    rows.drop(headerIndex + 1).takeWhile { it is ListRow.Item }.size

/** Resolves the group the task with [taskId] now sits in, from the nearest header above it. */
fun resolveDrop(rows: List<ListRow>, taskId: Long): DropResult? {
    val index = rows.indexOfFirst { it is ListRow.Item && it.task.id == taskId }
    if (index < 0) return null
    val headerIndex = (index downTo 0).firstOrNull { rows[it] is ListRow.Header } ?: return null
    val header = rows[headerIndex] as ListRow.Header
    val groupOrder = rows.drop(headerIndex + 1)
        .takeWhile { it is ListRow.Item }
        .map { (it as ListRow.Item).task.id }
    return DropResult(header.priority, groupOrder)
}
