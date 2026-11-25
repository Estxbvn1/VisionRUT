package cl.visionrut.app.data

import androidx.room.withTransaction
import cl.visionrut.app.domain.CalculadorRecurrencia.billingDate
import cl.visionrut.app.domain.noteLimited
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ReglaIncumplida(message: String) : IllegalStateException(message)

interface VisionRUTRepository {
    fun observeData(): Flow<VisionRUTData>
    suspend fun initialize()
    /** Una nota null mantiene las notas existentes. Una cadena vacía borra la nota individual. */
    suspend fun updateExpenses(ids: Set<Long>, categoryId: Long?, note: String?,
        confirmedReminders: Set<Long>): List<Long>
    suspend fun createCategory(name: String, icon: String, color: Long)
    suspend fun deleteCategory(category: Categoria)
    suspend fun saveReminder(original: Recordatorio?, expense: Egreso, service: String,
        date: LocalDate, frequency: String): Recordatorio
    suspend fun toggleReminder(reminder: Recordatorio, enabled: Boolean)
    suspend fun deleteReminder(id: Long)
    suspend fun getReminder(id: Long): Recordatorio?
    suspend fun getReminders(): List<Recordatorio>
    suspend fun markNotified(id: Long, due: Long)
}

class RoomVisionRUTRepository(private val database: VisionDatabase) : VisionRUTRepository {
    private val dao = database.dao()

    override fun observeData() =
        combine(dao.categories(), dao.expenses(), dao.reminders()) { categories, expenses, reminders ->
            val months = expenses.mapTo(mutableSetOf()) {
                YearMonth.from(LocalDate.ofEpochDay(it.day)).toString()
            }
            VisionRUTData(categories, expenses, reminders, months)
        }

    override suspend fun initialize() = DemoData.ensure(database)

    override suspend fun updateExpenses(ids: Set<Long>, categoryId: Long?, note: String?,
        confirmedReminders: Set<Long>): List<Long> = database.withTransaction {
        require(ids.isNotEmpty() && (note == null || ids.size == 1))
        require(categoryId == null || dao.category(categoryId) != null)
        val affected = if (categoryId == SUBSCRIPTIONS_ID) emptyList()
            else dao.remindersForExpenses(ids.toList()).map { it.id }
        if (!confirmedReminders.containsAll(affected))
            throw ReglaIncumplida("Hay un recordatorio asociado. Revisa el cambio y vuelve a confirmar.")
        affected.forEach { dao.deleteReminder(it) }
        if (note == null) dao.categorize(ids.toList(), categoryId)
        else dao.editExpense(ids.single(), categoryId, noteLimited(note).ifEmpty { null })
        affected
    }

    override suspend fun createCategory(name: String, icon: String, color: Long) {
        val clean = name.trim()
        require(clean.isNotEmpty() && clean.length <= 30)
        database.withTransaction {
            if (dao.hasCategoryName(clean)) throw ReglaIncumplida("Ya existe una categoría con ese nombre.")
            dao.saveCategory(Categoria(name = clean, icon = icon, color = color))
        }
    }

    override suspend fun deleteCategory(category: Categoria) {
        require(!category.included)
        dao.deleteCustomCategory(category.id)
    }

    override suspend fun saveReminder(original: Recordatorio?, expense: Egreso, service: String,
        date: LocalDate, frequency: String): Recordatorio = database.withTransaction {
        require(service.isNotBlank() && frequency in setOf("mensual", "anual"))
        require(dao.expense(expense.id)?.categoryId == SUBSCRIPTIONS_ID)
        val existing = dao.remindersForExpenses(listOf(expense.id)).singleOrNull()
        if (original == null && existing != null)
            throw ReglaIncumplida("Este cobro ya tiene un recordatorio.")
        if (original != null && existing?.id != original.id)
            throw ReglaIncumplida("El recordatorio ya no está disponible.")
        val today = LocalDate.now()
        if (date <= today && !(date == today && existing?.billingDate(today) == date))
            throw ReglaIncumplida("Elige una fecha de cobro desde mañana.")
        val scheduledDate = date.toEpochDay()
        val preserveCalendar = existing != null && existing.frequency == frequency &&
            (existing.anchorDay == scheduledDate || existing.billingDate() == date)
        val reminder = Recordatorio(
            id = existing?.id ?: 0, expenseId = expense.id, service = noteLimited(service.trim()),
            anchorDay = if (preserveCalendar) checkNotNull(existing).anchorDay else scheduledDate,
            frequency = frequency, enabled = existing?.enabled ?: true,
            lastNotifiedDue = existing?.lastNotifiedDue?.takeIf { preserveCalendar || it == scheduledDate } ?: -1,
        )
        if (existing == null) reminder.copy(id = dao.saveReminder(reminder))
        else {
            dao.updateReminder(reminder)
            reminder
        }
    }

    override suspend fun toggleReminder(reminder: Recordatorio, enabled: Boolean) =
        dao.toggleReminder(reminder.id, enabled)
    override suspend fun deleteReminder(id: Long) = dao.deleteReminder(id)
    override suspend fun getReminder(id: Long) = dao.reminder(id)
    override suspend fun getReminders() = dao.allReminders()
    override suspend fun markNotified(id: Long, due: Long) = dao.notified(id, due)
}
