package io.github.saikumar1.nextup.data

import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun add(task: Task): Long
    suspend fun update(task: Task)
    suspend fun delete(task: Task)
    suspend fun clearCompleted()
}

class RoomTaskRepository(private val dao: TaskDao) : TaskRepository {
    override fun observeTasks(): Flow<List<Task>> = dao.observeAll()
    override suspend fun add(task: Task): Long = dao.insert(task)
    override suspend fun update(task: Task) = dao.update(task)
    override suspend fun delete(task: Task) = dao.delete(task)
    override suspend fun clearCompleted() = dao.clearCompleted()
}
