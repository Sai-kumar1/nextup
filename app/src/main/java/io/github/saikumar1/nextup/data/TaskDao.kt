package io.github.saikumar1.nextup.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE dueDate = :day ORDER BY position ASC, createdAt ASC")
    fun observeForDate(day: Long): Flow<List<Task>>

    @Query(
        """
        SELECT dueDate, COUNT(*) AS total, SUM(isDone) AS done
        FROM tasks
        WHERE dueDate BETWEEN :from AND :to
        GROUP BY dueDate
        """,
    )
    fun observeDayCounts(from: Long, to: Long): Flow<List<DayCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    /** Updates several tasks in one transaction (used when reordering). */
    @Update
    suspend fun updateAll(tasks: List<Task>)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE isDone = 1 AND dueDate = :day")
    suspend fun clearCompleted(day: Long)
}
