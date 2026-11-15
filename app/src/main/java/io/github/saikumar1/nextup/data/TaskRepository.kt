package io.github.saikumar1.nextup.data

import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeForDate(day: Long): Flow<List<Task>>
    fun observeDayCounts(from: Long, to: Long): Flow<List<DayCount>>
    suspend fun add(task: Task): Long
    suspend fun update(task: Task)
    suspend fun delete(task: Task)
    suspend fun clearCompleted(day: Long)
}

class RoomTaskRepository(private val dao: TaskDao) : TaskRepository {
    override fun observeForDate(day: Long): Flow<List<Task>> = dao.observeForDate(day)
    override fun observeDayCounts(from: Long, to: Long): Flow<List<DayCount>> =
        dao.observeDayCounts(from, to)
    override suspend fun add(task: Task): Long = dao.insert(task)
    override suspend fun update(task: Task) = dao.update(task)
    override suspend fun delete(task: Task) = dao.delete(task)
    override suspend fun clearCompleted(day: Long) = dao.clearCompleted(day)
}
