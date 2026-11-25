package cl.visionrut.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.visionrut.app.MainActivity
import cl.visionrut.app.R
import cl.visionrut.app.ReminderReceiver
import cl.visionrut.app.data.Recordatorio
import cl.visionrut.app.data.RoomVisionRUTRepository
import cl.visionrut.app.data.VisionDatabase
import cl.visionrut.app.domain.CalculadorRecurrencia.billingDate
import cl.visionrut.app.domain.CalculadorRecurrencia.nextDue
import cl.visionrut.app.util.dateLabel
import java.time.ZonedDateTime

/** Operaciones de avisos que el ViewModel puede solicitar sin conocer Android. */
interface ProgramadorRecordatorios {
    fun schedule(reminder: Recordatorio)

    fun preview(reminder: Recordatorio): Boolean

    fun cancel(id: Long)

    fun restore(reminders: List<Recordatorio>)
}

/** Adaptador entre las solicitudes de la app y las alarmas de Android. */
class ProgramadorRecordatoriosAndroid(context: Context) : ProgramadorRecordatorios {
    private val appContext = context.applicationContext

    override fun schedule(reminder: Recordatorio) = ReminderScheduler.schedule(appContext, reminder)

    override fun preview(reminder: Recordatorio) =
        ReminderScheduler.post(appContext, reminder, reminder.billingDate().toEpochDay(), preview = true)

    override fun cancel(id: Long) = ReminderScheduler.cancel(appContext, id)

    override fun restore(reminders: List<Recordatorio>) {
        ReminderScheduler.createChannel(appContext)
        reminders.forEach(::schedule)
    }
}

/** Programa, restaura y cancela las alarmas locales de suscripciones. */
object ReminderScheduler {
    const val CHANNEL = "subscription_reminders"
    private const val PREVIEW_TAG = "preview"

    fun createChannel(context: Context) {
        context
            .getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                        CHANNEL,
                        "Recordatorios de servicios",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                    .apply {
                        description = "Avisos un día antes del próximo cobro de tus servicios"
                    }
            )
    }

    fun permitted(context: Context): Boolean {
        val runtime =
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
        val channel =
            context
                .getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CHANNEL)
        return runtime &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            channel?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun post(context: Context, reminder: Recordatorio, due: Long, preview: Boolean = false): Boolean {
        if (!reminder.enabled || !permitted(context)) return false
        val open = PendingIntent.getActivity(
            context, reminder.id.toInt(),
            Intent(context, MainActivity::class.java).putExtra("open_reminders", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val message = "Próximo cobro: ${dateLabel(due)}."
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFFF7900.toInt())
            .setContentTitle("Revisa ${reminder.service}")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$message Este aviso no paga ni cancela el servicio."
            ))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        return try {
            val tag = if (preview) PREVIEW_TAG else null
            NotificationManagerCompat.from(context).notify(tag, reminder.id.toInt(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun schedule(context: Context, reminder: Recordatorio) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val due = reminder.nextDue()
        val intent =
            Intent(context, ReminderReceiver::class.java)
                .putExtra("id", reminder.id)
                .putExtra("due", due.toEpochDay())
        val pending =
            PendingIntent.getBroadcast(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        alarm.cancel(pending)
        if (!reminder.enabled || !permitted(context)) return
        val target =
            due.minusDays(1)
                .atTime(9, 0)
                .atZone(ZonedDateTime.now().zone)
                .toInstant()
                .toEpochMilli()
        alarm.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            maxOf(target, System.currentTimeMillis() + 1000),
            pending,
        )
    }

    fun cancel(context: Context, id: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending =
            PendingIntent.getBroadcast(
                context,
                id.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
        if (pending != null) {
            context.getSystemService(AlarmManager::class.java).cancel(pending)
            pending.cancel()
        }
        NotificationManagerCompat.from(context).apply {
            cancel(id.toInt())
            cancel(PREVIEW_TAG, id.toInt())
        }
    }

    suspend fun restore(context: Context) {
        createChannel(context)
        RoomVisionRUTRepository(VisionDatabase.get(context)).getReminders().forEach {
            schedule(context, it)
        }
    }
}
