package cl.visionrut.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.visionrut.app.data.DemoData
import cl.visionrut.app.data.VisionDatabase
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import org.junit.rules.ExternalResource

class DemoDatabaseRule : ExternalResource() {
    private lateinit var database: VisionDatabase

    override fun before() {
        database =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext<Context>(),
                    VisionDatabase::class.java,
                )
                .build()
        runBlocking {
            DemoData.ensure(database)
            // Los recorridos de interfaz necesitan los mismos tipos de cobro incluso el día 1.
            val now = DemoData.mesDeReferencia()
            val today = LocalDate.now().toEpochDay()
            database.dao().insertExpenses(DemoData.month(now, now.atEndOfMonth()).map {
                it.copy(day = minOf(today, it.day))
            })
        }
        VisionDatabase.testInstance = database
    }

    override fun after() {
        VisionDatabase.testInstance = null
        database.close()
    }
}
