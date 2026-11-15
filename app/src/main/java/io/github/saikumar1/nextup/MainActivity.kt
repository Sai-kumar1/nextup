package io.github.saikumar1.nextup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.saikumar1.nextup.ui.TaskScreen
import io.github.saikumar1.nextup.ui.theme.NextUpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NextUpTheme {
                TaskScreen()
            }
        }
    }
}
