package cl.visionrut.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import cl.visionrut.app.ui.VisionRUTApp
import cl.visionrut.app.ui.theme.VisionRUTTheme

/** Punto de entrada de Android; inicia la interfaz y recibe accesos desde los avisos. */
class MainActivity : ComponentActivity() {
    private var reminderRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFFFF7900.toInt()),
            navigationBarStyle = SystemBarStyle.light(-1, 0xFF23262D.toInt()),
        )
        if (intent.getBooleanExtra("open_reminders", false)) reminderRequest++
        setContent { VisionRUTTheme { VisionRUTApp(reminderRequest) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_reminders", false)) reminderRequest++
    }
}
