package cl.visionrut.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VisionDao {
    @Query("SELECT * FROM categorias ORDER BY es_predefinida DESC, id")
    fun categories(): Flow<List<Categoria>>

    @Query("SELECT * FROM egresos ORDER BY fecha DESC, id") fun expenses(): Flow<List<Egreso>>
    @Query("SELECT * FROM recordatorios ORDER BY id") fun reminders(): Flow<List<Recordatorio>>
    @Query("SELECT EXISTS(SELECT 1 FROM egresos)") suspend fun hasExpenses(): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<Categoria>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpenses(expenses: List<Egreso>)

    @Insert suspend fun saveCategory(category: Categoria): Long

    @Query("SELECT nombre FROM categorias")
    suspend fun categoryNames(): List<String>

    @Query("UPDATE egresos SET categoria_id = :categoryId, nota = :note WHERE id = :id")
    suspend fun editExpense(id: Long, categoryId: Long?, note: String?)

    @Query("UPDATE egresos SET categoria_id = :categoryId WHERE id IN (:ids)")
    suspend fun categorize(ids: List<Long>, categoryId: Long?)

    @Query("DELETE FROM categorias WHERE id = :id AND es_predefinida = 0")
    suspend fun deleteCustomCategory(id: Long)

    @Query("SELECT * FROM categorias WHERE id = :id") suspend fun category(id: Long): Categoria?
    @Query("SELECT * FROM egresos WHERE id = :id") suspend fun expense(id: Long): Egreso?

    @Query("SELECT * FROM recordatorios WHERE egreso_id IN (:ids)")
    suspend fun remindersForExpenses(ids: List<Long>): List<Recordatorio>

    @Insert suspend fun saveReminder(reminder: Recordatorio): Long
    @Update suspend fun updateReminder(reminder: Recordatorio): Int
    @Query("SELECT * FROM recordatorios WHERE id = :id") suspend fun reminder(id: Long): Recordatorio?
    @Query("SELECT * FROM recordatorios") suspend fun allReminders(): List<Recordatorio>
    @Query("UPDATE recordatorios SET habilitado = :enabled WHERE id = :id")
    suspend fun toggleReminder(id: Long, enabled: Boolean)

    @Query("DELETE FROM recordatorios WHERE id = :id") suspend fun deleteReminder(id: Long)
    @Query("UPDATE recordatorios SET ultimo_cobro_notificado = :due WHERE id = :id")
    suspend fun notified(id: Long, due: Long)
}
