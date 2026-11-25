package cl.visionrut.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Base Room de categorías, egresos y recordatorios. */
@Database(
    entities = [Categoria::class, Egreso::class, Recordatorio::class],
    version = 5,
    exportSchema = true,
)
abstract class VisionDatabase : RoomDatabase() {
    abstract fun dao(): VisionDao

    companion object {
        @Volatile private var instance: VisionDatabase? = null
        @androidx.annotation.VisibleForTesting
        @Volatile
        internal var testInstance: VisionDatabase? = null

        fun get(context: Context): VisionDatabase =
            testInstance
                ?: instance
                ?: synchronized(this) {
                    instance
                        ?: Room.databaseBuilder(
                                context.applicationContext,
                                VisionDatabase::class.java,
                                "visionrut.db",
                            )
                            .addMigrations(Migracion1a2, Migracion2a3, Migracion3a4, Migracion4a5)
                            .build()
                            .also { instance = it }
                }
    }
}
