package io.github.saikumar1.nextup

import android.app.Application
import io.github.saikumar1.nextup.data.AppDatabase
import io.github.saikumar1.nextup.data.RoomTaskRepository
import io.github.saikumar1.nextup.data.TaskRepository
import io.github.saikumar1.nextup.data.ThemePreferences

class NextUpApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    private val database by lazy { AppDatabase.build(app) }
    val taskRepository: TaskRepository by lazy { RoomTaskRepository(database.taskDao()) }
    val themePreferences: ThemePreferences by lazy { ThemePreferences(app) }
}
