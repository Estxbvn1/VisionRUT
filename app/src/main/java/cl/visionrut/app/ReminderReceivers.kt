package cl.visionrut.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cl.visionrut.app.data.RoomVisionRUTRepository
import cl.visionrut.app.data.VisionDatabase
import cl.visionrut.app.domain.CalculadorRecurrencia.nextDue
import cl.visionrut.app.notifications.ReminderScheduler
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Recibe la alarma de un servicio, muestra el aviso y programa su siguiente ocurrencia. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = RoomVisionRUTRepository(VisionDatabase.get(context))
                val reminder =
                    repository.getReminder(intent.getLongExtra("id", -1)) ?: return@launch
                val due = intent.getLongExtra("due", -1)
                if (!reminder.enabled || due <= reminder.lastNotifiedDue) return@launch

                if (!ReminderScheduler.permitted(context)) return@launch
                if (due != reminder.nextDue(LocalDate.now()).toEpochDay()) {
                    ReminderScheduler.schedule(context, reminder)
                    return@launch
                }

                if (!ReminderScheduler.post(context, reminder, due)) return@launch
                repository.markNotified(reminder.id, due)
                repository.getReminder(reminder.id)?.let { ReminderScheduler.schedule(context, it) }
            } finally {
                result.finish()
            }
        }
    }
}

/** Restaura los avisos al reiniciar el teléfono o cambiar su calendario. */
class RestoreRemindersReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action !in
                setOf(
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                )
        )
            return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ReminderScheduler.restore(context)
            } finally {
                result.finish()
            }
        }
    }
}
