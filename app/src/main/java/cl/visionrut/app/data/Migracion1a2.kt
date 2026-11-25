package cl.visionrut.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Conserva las referencias mientras convierte las claves de categoría de texto a enteros. */
internal object Migracion1a2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE categorias (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL,
            icono TEXT NOT NULL, color INTEGER NOT NULL, es_predefinida INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE egresos (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, fecha INTEGER NOT NULL,
            descripcion TEXT NOT NULL, tipo_operacion TEXT NOT NULL, monto INTEGER NOT NULL,
            categoria_id INTEGER, nota TEXT,
            FOREIGN KEY(categoria_id) REFERENCES categorias(id) ON UPDATE NO ACTION ON DELETE SET NULL)""")
        db.execSQL("CREATE INDEX index_egresos_categoria_id ON egresos(categoria_id)")
        db.execSQL("""CREATE TABLE recordatorios (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, egreso_id INTEGER NOT NULL,
            servicio TEXT NOT NULL, fecha_referencia INTEGER NOT NULL, frecuencia TEXT NOT NULL,
            habilitado INTEGER NOT NULL, ultimo_cobro_notificado INTEGER NOT NULL,
            FOREIGN KEY(egreso_id) REFERENCES egresos(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("CREATE UNIQUE INDEX index_recordatorios_egreso_id ON recordatorios(egreso_id)")

        db.execSQL("CREATE TEMP TABLE mapa_categorias (anterior TEXT PRIMARY KEY, nueva INTEGER NOT NULL)")
        val predefined = IncludedCategories.associateBy { it.icon }
        var nextId = IncludedCategories.maxOf { it.id } + 1
        db.query("SELECT id, name, icon, color, included FROM categories ORDER BY position, id").use { rows ->
            while (rows.moveToNext()) {
                val old = rows.getString(0)
                val id = predefined[old]?.id ?: nextId++
                db.execSQL("INSERT INTO mapa_categorias VALUES (?, ?)", arrayOf(old, id))
                db.execSQL("INSERT INTO categorias VALUES (?, ?, ?, ?, ?)",
                    arrayOf(id, rows.getString(1), rows.getString(2), rows.getLong(3), rows.getInt(4)))
            }
        }
        db.execSQL("""INSERT INTO egresos
            SELECT e.id, e.day, e.description, e.operation, e.amount, m.nueva, NULLIF(e.note, '')
            FROM expenses e LEFT JOIN mapa_categorias m ON m.anterior = e.categoryId""")
        // Solo se completan notas vacías reconocibles de los movimientos de demostración anteriores.
        db.execSQL("""UPDATE egresos SET nota = CASE
            WHEN descripcion LIKE '%MERCADOPAGO%' THEN 'Compra por internet'
            WHEN descripcion LIKE '%ALMACÉN%' THEN 'Pan y huevos para la once'
            WHEN descripcion LIKE '%FERIA%' THEN 'Fruta y verduras de la semana'
            WHEN descripcion LIKE '%RECARGA BIP%' THEN 'Pasajes para ir a clases'
            WHEN descripcion LIKE '%ARENA PARA MASCOTAS%' THEN 'Arena para el gato'
            WHEN descripcion LIKE '%ELECTRICIDAD%' THEN 'Mi parte de la cuenta de luz'
            WHEN descripcion LIKE '%LIBRERÍA%' THEN 'Materiales para clases'
            WHEN descripcion LIKE '%SUPERMERCADO%' THEN 'Compra de la semana'
            WHEN descripcion LIKE '%VIDEO MENSUAL%' THEN 'Plan familiar de video' END
            WHERE nota IS NULL AND id % 3 != 0""")
        // Por cada cobro válido conservamos el aviso guardado más recientemente, incluso si está pausado.
        db.execSQL("""INSERT INTO recordatorios
            SELECT r.id, r.expenseId, r.service, r.anchorDay,
                CASE r.frequency WHEN 'annual' THEN 'anual' ELSE 'mensual' END, r.enabled, r.lastNotifiedDue
            FROM reminders r JOIN egresos e ON e.id = r.expenseId
            WHERE e.categoria_id = $SUBSCRIPTIONS_ID AND r.id =
                (SELECT MAX(r2.id) FROM reminders r2 WHERE r2.expenseId = r.expenseId)""")
        // Un PendingIntent de la versión anterior nunca debe apuntar a un nuevo recordatorio.
        db.execSQL("""INSERT INTO sqlite_sequence(name, seq)
            SELECT 'recordatorios', 0 WHERE NOT EXISTS
            (SELECT 1 FROM sqlite_sequence WHERE name = 'recordatorios')""")
        db.execSQL("""UPDATE sqlite_sequence SET seq = MAX(seq, COALESCE(
            (SELECT MAX(seq) FROM sqlite_sequence WHERE name = 'reminders'), 0))
            WHERE name = 'recordatorios'""")
        db.execSQL("DROP TABLE reminders")
        db.execSQL("DROP TABLE expenses")
        db.execSQL("DROP TABLE categories")
        db.execSQL("DROP TABLE seed_markers")
        db.execSQL("DROP TABLE mapa_categorias")
    }
}
