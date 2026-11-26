package cl.visionrut.app

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.visionrut.app.data.Migracion1a2
import cl.visionrut.app.data.Migracion2a3
import cl.visionrut.app.data.Migracion3a4
import cl.visionrut.app.data.Migracion4a5
import cl.visionrut.app.data.RoomVisionRUTRepository
import cl.visionrut.app.data.ReglaIncumplida
import cl.visionrut.app.data.DemoData
import cl.visionrut.app.data.VisionDatabase
import cl.visionrut.app.data.Egreso
import cl.visionrut.app.data.IncludedCategories
import cl.visionrut.app.data.Categoria
import cl.visionrut.app.data.Recordatorio
import cl.visionrut.app.domain.CalculadorResumen.expensesInMonth
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    @Test
    fun updatingTheDemoKeepsPersonalNotesAndTheReminderLink() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "cartola-${System.nanoTime()}.db"
        val base = 2026 * 100000L + 8 * 1000L
        val video = Egreso(base + 6, LocalDate.of(2026, 8, 1).toEpochDay(), "VIDEO MENSUAL",
            "Cobro recurrente", 6990, 6L, "Plan familiar de video")
        val music = Egreso(base + 21, LocalDate.of(2026, 8, 2).toEpochDay(), "MÚSICA MENSUAL",
            "Cobro recurrente", 3000, 6L, "Mi propia nota")
        // v2 y v3 comparten el esquema físico. Preparamos los valores publicados en v2.
        val initial = Room.databaseBuilder(context, VisionDatabase::class.java, name).build()
        val reminderId = try {
            initial.dao().insertCategories(IncludedCategories)
            initial.dao().insertExpenses(listOf(video, music))
            initial.dao().saveReminder(Recordatorio(expenseId = video.id, service = "Video mensual",
                anchorDay = LocalDate.of(2026, 9, 1).toEpochDay(), frequency = "mensual", enabled = false))
        } finally {
            initial.close()
        }
        context.openOrCreateDatabase(name, 0, null).use { it.version = 2 }
        val db = Room.databaseBuilder(context, VisionDatabase::class.java, name)
            .addMigrations(Migracion2a3, Migracion3a4, Migracion4a5).build()
        try {
            val dao = db.dao()
            DemoData.ensure(db)
            assertEquals(DemoData.NETFLIX, dao.expense(video.id)!!.description)
            assertEquals(music, dao.expense(music.id))
            val saved = dao.reminder(reminderId)!!
            assertEquals(video.id, saved.expenseId)
            assertEquals("Netflix", saved.service)
            assertFalse(saved.enabled)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migrationPreservesLinksAndConfirmsLinkedRemindersBeforeRecategorizing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${System.nanoTime()}.db"
        context.openOrCreateDatabase(name, 0, null).use { old ->
            old.execSQL("CREATE TABLE categories(id TEXT PRIMARY KEY, name TEXT, icon TEXT, color INTEGER, included INTEGER, position INTEGER)")
            old.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY, day INTEGER, description TEXT, operation TEXT, amount INTEGER, categoryId TEXT, note TEXT)")
            old.execSQL("CREATE TABLE reminders(id INTEGER PRIMARY KEY AUTOINCREMENT, expenseId INTEGER, service TEXT, anchorDay INTEGER, frequency TEXT, enabled INTEGER, lastNotifiedDue INTEGER)")
            old.execSQL("CREATE TABLE seed_markers(`key` TEXT PRIMARY KEY)")
            old.execSQL("INSERT INTO categories VALUES ('subscriptions','Suscripciones','subscriptions',1,1,5),('uuid','Clases','study',2,0,20)")
            old.execSQL("INSERT INTO expenses VALUES (11,20000,'VIDEO','Compra',7000,'subscriptions','Plan de casa'),(12,20001,'LIBRO','Compra',3000,'uuid','Taller')")
            old.execSQL("INSERT INTO reminders VALUES (1,11,'Original',21000,'monthly',1,-1),(2,11,'Plan familiar',21000,'annual',0,-1)")
            old.version = 1
        }
        val db = Room.databaseBuilder(context, VisionDatabase::class.java, name)
            .addMigrations(Migracion1a2, Migracion2a3, Migracion3a4, Migracion4a5).build()
        try {
            val dao = db.dao()
            val repo = RoomVisionRUTRepository(db)
            val reminder = dao.allReminders().single()
            val custom = dao.categories().first().single { !it.included }
            assertEquals(2L, reminder.id)
            assertEquals("anual", reminder.frequency)
            assertEquals(custom.id, dao.expense(12)!!.categoryId)
            assertEquals("Taller", dao.expense(12)!!.note)

            val source = dao.expense(11)!!
            val nextMonth = source.copy(id = 13, day = source.day + 31, note = null)
            dao.insertExpenses(listOf(nextMonth))
            val duplicate = runCatching {
                repo.saveReminder(null, nextMonth, "Otro nombre", LocalDate.now().plusDays(10), "mensual")
            }
            assertTrue(duplicate.exceptionOrNull() is ReglaIncumplida)
            assertTrue(repo.updateExpenses(setOf(11L), null, null, emptySet()).isEmpty())
            assertEquals(reminder.copy(expenseId = 13), repo.getReminder(reminder.id))
            val ids = setOf(13L, 12L)
            val rejected = runCatching { repo.updateExpenses(ids, null, null, emptySet()) }
            assertTrue(rejected.exceptionOrNull() is ReglaIncumplida)
            assertNotNull(dao.reminder(2))
            assertEquals(custom.id, dao.expense(12)!!.categoryId)
            assertEquals(listOf(2L), repo.updateExpenses(ids, null, null, setOf(2L)))
            assertNull(dao.reminder(2))
            assertNull(dao.expense(11)!!.categoryId)
            assertEquals("Plan de casa", dao.expense(11)!!.note)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun refreshingDemoDetailsPreservesPersonalEdits() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VisionDatabase::class.java).build()
        try {
            DemoData.ensure(db)
            val dao = db.dao()
            val row = dao.expenses().first().first()
            val custom = Categoria(90L, "Mis compras", "shopping", 0xFF1685FF)
            dao.saveCategory(custom)
            dao.editExpense(row.id, custom.id, "Nota que debo conservar")
            DemoData.ensure(db)
            val refreshed = dao.expense(row.id)!!
            assertEquals(custom.id, refreshed.categoryId)
            assertEquals("Nota que debo conservar", refreshed.note)
            assertEquals(row.amount, refreshed.amount)
            assertEquals(row.id, refreshed.id)
            assertEquals(custom, dao.category(custom.id))
            assertEquals(row.description, refreshed.description)
        } finally {
            db.close()
        }
    }

    @Test
    fun seedEditsBulkDeleteAndReminderSurviveReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "verification-${System.nanoTime()}.db"
        var db = Room.databaseBuilder(context, VisionDatabase::class.java, name).build()
        try {
            DemoData.ensure(db)
            DemoData.ensure(db)
            var dao = db.dao()
            val original = dao.expenses().first()
            assertEquals(original.size, original.distinctBy { it.id }.size)
            val current = expensesInMonth(original, DemoData.mesDeReferencia())
            assertTrue(current.isNotEmpty())
            val pending = current.filter { it.categoryId == null }
            dao.categorize(pending.map { it.id }, 7L)
            assertTrue(
                expensesInMonth(dao.expenses().first(), DemoData.mesDeReferencia()).none {
                    it.categoryId == null
                }
            )
            val custom = Categoria(91L, "Salud", "health", 0xFF25B84B)
            dao.saveCategory(custom)
            dao.editExpense(current.first().id, custom.id, "Comida gato")
            val reminder =
                Recordatorio(
                    expenseId = original.first { it.description == DemoData.SPOTIFY }.id,
                    service = "Servicio de prueba",
                    anchorDay = LocalDate.now().plusDays(5).toEpochDay(),
                    frequency = "anual",
                    enabled = false,
                )
            val id = dao.saveReminder(reminder)
            db.close()
            db = Room.databaseBuilder(context, VisionDatabase::class.java, name).build()
            dao = db.dao()
            assertEquals("Comida gato", dao.expense(current.first().id)!!.note)
            assertEquals(91L, dao.expense(current.first().id)!!.categoryId)
            assertEquals("anual", dao.reminder(id)!!.frequency)
            assertFalse(dao.reminder(id)!!.enabled)
            db.withTransaction {
                dao.deleteCustomCategory(custom.id)
            }
            assertNull(dao.category(custom.id))
            assertNull(dao.expense(current.first().id)!!.categoryId)
            assertEquals("Comida gato", dao.expense(current.first().id)!!.note)
            dao.editExpense(current.first().id, 8L, null)
            assertNull(dao.expense(current.first().id)!!.note)
            dao.deleteCustomCategory(1L)
            assertNotNull(dao.category(1L))
            val result = dao.expenses().first()
            original.forEach { row ->
                val saved = result.first { it.id == row.id }
                assertEquals(row.amount, saved.amount)
                assertEquals(row.description, saved.description)
                assertEquals(row.day, saved.day)
                assertEquals(row.operation, saved.operation)
            }
            assertEquals(original.size, result.size)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
