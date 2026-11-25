package cl.visionrut.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import cl.visionrut.app.data.DemoData
import cl.visionrut.app.data.VisionDatabase
import cl.visionrut.app.data.Recordatorio
import kotlinx.coroutines.flow.first
import cl.visionrut.app.domain.CalculadorRecurrencia.nextDue
import cl.visionrut.app.notifications.ReminderScheduler
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationTest {
    @get:Rule(order = 0) val database = DemoDatabaseRule()
    @get:Rule(order = 1)
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Test
    fun localNoticeIsDeliveredAndTheNextCalendarOccurrenceIsScheduled() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = VisionDatabase.get(context)
        DemoData.ensure(db)
        val dao = db.dao()
        val due = LocalDate.now().plusDays(1)
        val source = dao.expenses().first().first { it.description == DemoData.SPOTIFY }
        val reminder =
            Recordatorio(
                expenseId = source.id,
                service = "Verificación de aviso",
                anchorDay = due.toEpochDay(),
                frequency = "mensual",
            )
        val id = dao.saveReminder(reminder)
        val saved = reminder.copy(id = id)
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            ReminderScheduler.createChannel(context)
            assertTrue(ReminderScheduler.permitted(context))
            ReminderScheduler.schedule(context, saved)
            context.sendBroadcast(
                Intent(context, ReminderReceiver::class.java)
                    .putExtra("id", id)
                    .putExtra("due", due.toEpochDay())
            )
            var delivered = false
            for (attempt in 0..99) {
                if (dao.reminder(id)?.lastNotifiedDue == due.toEpochDay()) {
                    delivered = true
                    break
                }
                delay(100)
            }
            assertTrue("El receptor debe procesar el aviso local", delivered)
            val notice = manager.activeNotifications.single { it.id == id.toInt() }.notification
            assertEquals(saved.service, notice.extras.getString(android.app.Notification.EXTRA_TITLE))
            assertEquals("Próximo cobro: ${cl.visionrut.app.util.dateLabel(due.toEpochDay())}",
                notice.extras.getString(android.app.Notification.EXTRA_TEXT))
            assertEquals(due.plusMonths(1), dao.reminder(id)!!.nextDue())
            dao.toggleReminder(id, false)
            ReminderScheduler.schedule(context, dao.reminder(id)!!)
            assertFalse(dao.reminder(id)!!.enabled)
        } finally {
            ReminderScheduler.schedule(context, saved.copy(enabled = false))
            manager.cancel(id.toInt())
            db.openHelper.writableDatabase.execSQL(
                "DELETE FROM recordatorios WHERE id = ?",
                arrayOf(id),
            )
        }
    }
}
