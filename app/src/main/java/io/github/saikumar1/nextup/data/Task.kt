package io.github.saikumar1.nextup.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "tasks", indices = [Index("dueDate")])
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    /** The day this task is planned for, as [LocalDate.toEpochDay]. */
    val dueDate: Long = LocalDate.now().toEpochDay(),
    val priority: Priority = Priority.NONE,
    /** Manual order within the task's priority group (lower first). New tasks go last. */
    val position: Long = System.currentTimeMillis(),
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(dueDate)
}

/** Number of tasks (and how many are done) planned for one day. */
data class DayCount(
    val dueDate: Long,
    val total: Int,
    val done: Int,
)
