package io.github.saikumar1.nextup.data

import androidx.room.TypeConverter

/** Optional task priority. Stored as [level] so the column sorts naturally. */
enum class Priority(val level: Int) {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    ;

    companion object {
        /** Display / grouping order: most important first, "no priority" last. */
        val displayOrder = listOf(HIGH, MEDIUM, LOW, NONE)

        fun fromLevel(level: Int): Priority = entries.firstOrNull { it.level == level } ?: NONE
    }
}

class Converters {
    @TypeConverter
    fun priorityToLevel(priority: Priority): Int = priority.level

    @TypeConverter
    fun levelToPriority(level: Int): Priority = Priority.fromLevel(level)
}
