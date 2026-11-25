package cl.visionrut.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "nombre") val name: String,
    @ColumnInfo(name = "icono") val icon: String,
    val color: Long,
    @ColumnInfo(name = "es_predefinida") val included: Boolean = false,
)

@Entity(
    tableName = "egresos",
    foreignKeys = [ForeignKey(
        entity = Categoria::class, parentColumns = ["id"], childColumns = ["categoria_id"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("categoria_id")],
)
data class Egreso(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "fecha") val day: Long,
    @ColumnInfo(name = "descripcion") val description: String,
    @ColumnInfo(name = "tipo_operacion") val operation: String,
    @ColumnInfo(name = "monto") val amount: Long,
    @ColumnInfo(name = "categoria_id") val categoryId: Long? = null,
    @ColumnInfo(name = "nota") val note: String? = null,
)

@Entity(
    tableName = "recordatorios",
    foreignKeys = [ForeignKey(
        entity = Egreso::class, parentColumns = ["id"], childColumns = ["egreso_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["egreso_id"], unique = true)],
)
data class Recordatorio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "egreso_id") val expenseId: Long,
    @ColumnInfo(name = "servicio") val service: String,
    @ColumnInfo(name = "fecha_referencia") val anchorDay: Long,
    @ColumnInfo(name = "frecuencia") val frequency: String,
    @ColumnInfo(name = "habilitado") val enabled: Boolean = true,
    @ColumnInfo(name = "ultimo_cobro_notificado") val lastNotifiedDue: Long = -1,
)

data class VisionRUTData(
    val categories: List<Categoria> = emptyList(),
    val expenses: List<Egreso> = emptyList(),
    val reminders: List<Recordatorio> = emptyList(),
    val months: Set<String> = emptySet(),
)
