package io.github.saikumar1.nextup.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small user settings, stored in SharedPreferences. */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _darkMode = MutableStateFlow(readDarkMode())

    /** `true` = dark, `false` = light, `null` = follow the system (until the toggle is first used). */
    val darkMode: StateFlow<Boolean?> = _darkMode.asStateFlow()

    private val _confirmPriorityChange = MutableStateFlow(prefs.getBoolean(KEY_CONFIRM_PRIORITY, true))

    /** Whether dragging a task into another priority group asks for confirmation first. */
    val confirmPriorityChange: StateFlow<Boolean> = _confirmPriorityChange.asStateFlow()

    fun setDarkMode(dark: Boolean) {
        prefs.edit().putString(KEY_THEME, if (dark) DARK else LIGHT).apply()
        _darkMode.value = dark
    }

    fun setConfirmPriorityChange(confirm: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_PRIORITY, confirm).apply()
        _confirmPriorityChange.value = confirm
    }

    private fun readDarkMode(): Boolean? = when (prefs.getString(KEY_THEME, null)) {
        DARK -> true
        LIGHT -> false
        else -> null
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_CONFIRM_PRIORITY = "confirm_priority_change"
        const val DARK = "dark"
        const val LIGHT = "light"
    }
}
