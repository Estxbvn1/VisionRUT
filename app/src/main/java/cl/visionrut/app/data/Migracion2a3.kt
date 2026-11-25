package cl.visionrut.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

/** Reconoce las muestras publicadas antes de renovar un egreso con su mismo identificador. */
internal object Migracion2a3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val hoy = LocalDate.now()
        // El aviso de video se creaba con el egreso del mes de la primera carga.
        val primeraCarga = db.query("""SELECT e.fecha FROM recordatorios r JOIN egresos e ON e.id=r.egreso_id
            WHERE e.descripcion='VIDEO MENSUAL' AND e.id % 1000=6 AND r.servicio='Video mensual'
            ORDER BY r.id LIMIT 1""").use { c ->
            if (c.moveToFirst()) YearMonth.from(LocalDate.ofEpochDay(c.getLong(0))) else null
        }
        for (mes in 8..11) {
            val periodo = YearMonth.of(2026, mes)
            if (periodo > YearMonth.from(hoy)) continue
            val nuevos = DemoData.month(periodo, periodo.atEndOfMonth()).associateBy { it.id }
            val offset = if (periodo == primeraCarga || periodo == YearMonth.from(hoy)) 0 else 1
            val anteriores = mesVersion1(db, periodo) + mesVersion2(periodo, offset, periodo.atEndOfMonth())
            for (anterior in anteriores) {
                val nuevo = nuevos[anterior.id] ?: continue
                // Una edición personal, incluso borrar una nota, excluye la fila de la renovación.
                val fecha = if (nuevo.day <= hoy.toEpochDay()) nuevo.day else anterior.day
                db.execSQL("""UPDATE egresos SET fecha=?, descripcion=?, tipo_operacion=?, monto=?, categoria_id=?, nota=?
                    WHERE id=? AND fecha=? AND descripcion=? AND tipo_operacion=? AND monto=?
                    AND categoria_id IS ? AND nota IS ? AND
                    (NOT EXISTS (SELECT 1 FROM recordatorios WHERE egreso_id=egresos.id) OR
                     (categoria_id=6 AND ?=6 AND EXISTS (SELECT 1 FROM recordatorios
                      WHERE egreso_id=egresos.id AND servicio='Video mensual' AND frecuencia='mensual')))""",
                    arrayOf(fecha, nuevo.description, nuevo.operation, nuevo.amount, nuevo.categoryId, nuevo.note,
                        anterior.id, anterior.day, anterior.description, anterior.operation, anterior.amount,
                        anterior.categoryId, anterior.note, nuevo.categoryId))
            }
        }
        db.execSQL("""UPDATE recordatorios SET servicio='Netflix' WHERE servicio='Video mensual'
            AND egreso_id IN (SELECT id FROM egresos WHERE descripcion=?)""", arrayOf(DemoData.NETFLIX))
        val nube = DemoData.respaldoAnual
        db.execSQL("""UPDATE egresos SET descripcion=?, tipo_operacion=?, monto=?, nota=?
            WHERE id=? AND fecha=? AND descripcion='LICENCIA SOFTWARE ANUAL' AND tipo_operacion='Cobro recurrente'
            AND monto=12000 AND categoria_id=6 AND nota='Licencia de respaldo de archivos'""",
            arrayOf(nube.description, nube.operation, nube.amount, nube.note, nube.id, nube.day))
        db.execSQL("""UPDATE recordatorios SET servicio='Respaldo en la nube' WHERE servicio='Software anual'
            AND egreso_id IN (SELECT id FROM egresos WHERE id=? AND descripcion=?)""",
            arrayOf(nube.id, nube.description))
    }


    // En v1 el día dependía del día de instalación. El resto de la firma bancaria era estable.
    private val comerciosV1 = mapOf(
        1L to listOf("SUPERMERCADO LIDER", "FERIA BARRIO", "ALMACÉN DON JUAN", "RESTAURANTE LA PLAZA", "PANADERÍA SAN JOSÉ", "SUPERMERCADO JUMBO"),
        2L to listOf("RECARGA BIP", "VIAJE METRO", "TRANSPORTE URBANO", "VIAJE EN TAXI", "RECARGA TRANSPORTE"),
        3L to listOf("PAGO ELECTRICIDAD", "PAGO AGUA POTABLE", "PAGO INTERNET HOGAR", "CUENTA TELEFONÍA"),
        4L to listOf("LIBRERÍA CENTRAL", "COMPRA FERRETERÍA", "TIENDA DE ROPA", "COMERCIO ELECTRÓNICO", "COMPRA FARMACIA"),
        5L to listOf("ENTRADAS CINE", "ENTRADA MUSEO", "ENTRADAS CONCIERTO", "TEATRO MUNICIPAL"),
        7L to listOf("TIENDA MASCOTAS", "ALIMENTO MASCOTAS", "VETERINARIA BARRIO", "ARENA PARA MASCOTAS"),
    )

    private fun mesVersion1(db: SupportSQLiteDatabase, periodo: YearMonth): List<Egreso> {
        val base = baseId(periodo)
        val categoriasMes = listOf(null, null, 1L, 7L, 2L, 6L, 1L, 3L, 4L, 5L, 1L, 2L, 3L, 4L, 5L, 7L, 8L, 2L, 1L, 8L)
        val pesosMes = listOf(15, 5, 20, 6, 10, 10, 15, 12, 12, 12, 15, 12, 8, 8, 8, 4, 7, 8, 10, 3)
        val categoriasHistoricas = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 1L, 2L, 7L, 8L)
        val pesosHistoricos = listOf(20, 12, 10, 10, 10, 5, 5, 3, 10, 8, 5, 2)
        return buildList {
            db.query("""SELECT id, fecha, descripcion, tipo_operacion, monto, categoria_id, nota
                FROM egresos WHERE id BETWEEN ? AND ? AND fecha BETWEEN ? AND ?""",
                arrayOf(base + 1, base + 20, periodo.atDay(1).toEpochDay(), periodo.atEndOfMonth().toEpochDay())).use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val i = (id - base - 1).toInt()
                    val categoria = if (c.isNull(5)) null else c.getLong(5)
                    val glosa = c.getString(2)
                    val pesos = c.getLong(4)
                    val esMes = categoria == categoriasMes[i] && pesos == pesosMes[i] * 1000L
                    val esHist = i in categoriasHistoricas.indices && categoria == categoriasHistoricas[i] &&
                        (pesos == pesosHistoricos[i] * 1000L || i == 6 && pesos in listOf(15000L, 25000L))
                    if (!esMes && !esHist) continue
                    val glosas = comerciosV1[categoria]
                    val esperada = when {
                        glosas != null -> glosas[(periodo.monthValue + 2 * i + periodo.year) % glosas.size]
                        categoria == 6L -> "VIDEO MENSUAL · SERVICIO DIGITAL"
                        categoria == null -> if (i == 0) "COMPRA MERCADOPAGO" else "COMPRA TARJETA"
                        i == 7 || i == 16 -> "RETIRO CAJERO"
                        else -> "TRANSFERENCIA ENVIADA"
                    }
                    if (glosa != esperada) continue
                    val nota = if (c.isNull(6)) null else c.getString(6)
                    if (nota != null && (id % 3 == 0L || nota != notaAutomaticaV1(glosa))) continue
                    add(Egreso(id, c.getLong(1), glosa, c.getString(3), pesos, categoria, nota))
                }
            }
        }
    }

    private fun notaAutomaticaV1(glosa: String): String? = when {
        "MERCADOPAGO" in glosa -> "Compra por internet"
        "ALMACÉN" in glosa -> "Pan y huevos para la once"
        "FERIA" in glosa -> "Fruta y verduras de la semana"
        "RECARGA BIP" in glosa -> "Pasajes para ir a clases"
        "ARENA PARA MASCOTAS" in glosa -> "Arena para el gato"
        "ELECTRICIDAD" in glosa -> "Mi parte de la cuenta de luz"
        "LIBRERÍA" in glosa -> "Materiales para clases"
        "SUPERMERCADO" in glosa -> "Compra de la semana"
        "VIDEO MENSUAL" in glosa -> "Plan familiar de video"
        else -> null
    }

    // Copia del generador publicado en v2. La migración depende de su secuencia y semilla exactas.
    private data class Compra(
        val dia: Int, val categoria: Long, val comercio: String, val pesos: Int,
        val nota: String? = null, val operacion: String = "Compra con tarjeta",
    )

    private val compras = listOf(
        Compra(2, 4, "MERCADOPAGO", 12_490, "Funda para el celular"),
        Compra(4, 1, "COMPRA TARJETA", 3_500, "Colación entre clases"),
        Compra(3, 1, "ALMACÉN JUANITA", 4_200, "Pan, huevos y un litro de leche"),
        Compra(5, 7, "TIENDA MASCOTAS", 8_500, "Comida para el gato"),
        Compra(2, 2, "RECARGA BIP", 6_000, "Pasajes de ida y vuelta", "Recarga"),
        Compra(1, SUBSCRIPTIONS_ID, "VIDEO MENSUAL", 6_990, "Plan familiar de video", "Cobro recurrente"),
        Compra(7, 1, "FERIA DEL BARRIO", 7_600, "Frutas y verduras de la semana"),
        Compra(8, 3, "PAGO ELECTRICIDAD", 9_300, "Mi parte de la luz", "Pago de servicio"),
        Compra(9, 4, "LIBRERÍA CENTRAL", 4_400),
        Compra(10, 5, "CINE", 5_500),
        Compra(12, 1, "ALMACÉN JUANITA", 2_700),
        Compra(13, 2, "RECARGA BIP", 5_000, null, "Recarga"),
        Compra(14, 3, "INTERNET HOGAR", 9_990, null, "Pago de servicio"),
        Compra(16, 4, "FARMACIA DEL BARRIO", 3_200),
        Compra(18, 1, "CAFETERÍA CAMPUS", 2_800, "Café después del taller"),
        Compra(19, 7, "ARENA PARA MASCOTAS", 6_400),
        Compra(20, 8, "TRANSFERENCIA A CAMILA", 4_500, "Mi parte del almuerzo", "Transferencia enviada"),
        Compra(22, 2, "RECARGA BIP", 4_000, null, "Recarga"),
        Compra(24, 1, "SUPERMERCADO", 14_800, "Arroz, fideos y despensa"),
        Compra(27, 1, "PANADERÍA SAN JOSÉ", 2_100),
        Compra(2, SUBSCRIPTIONS_ID, "MÚSICA MENSUAL", 3_000, null, "Cobro recurrente"),
        Compra(25, 5, "TEATRO MUNICIPAL", 5_500, "Entrada con descuento estudiante"),
        Compra(28, 4, "FERRETERÍA", 3_700, "Ampolleta para la pieza"),
    )
    private fun baseId(month: YearMonth) = month.year * 100000L + month.monthValue * 1000L

    private fun mesVersion2(month: YearMonth, offset: Int, today: LocalDate): List<Egreso> {
        val base = baseId(month)
        val count = 21 + month.monthValue % 3
        val temporada = month.monthValue % 4
        return compras.take(count).mapIndexedNotNull { i, compra ->
            val detalle = when (i) {
                0 -> listOf(
                    compra, compra.copy(pesos = 7_990, nota = "Láminas para el taller"),
                    compra.copy(pesos = 4_990, nota = "Cable de carga del teléfono"),
                    compra.copy(pesos = 9_490, nota = "Repuesto para la bicicleta"),
                )[temporada]
                8 -> listOf(compra, Compra(9, 4, "FOTOCOPIAS CAMPUS", 2_350, "Guías para el certamen"),
                    Compra(9, 4, "MERCERÍA EL SOL", 3_400), Compra(9, 4, "LIBRERÍA USADOS", 5_200, "Novela de segunda mano"))[temporada]
                9 -> listOf(compra, Compra(10, 5, "CENTRO CULTURAL", 3_500),
                    Compra(11, 5, "MUSEO", 2_500, "Entrada de estudiante"), compra)[temporada]
                16 -> listOf(compra, compra.copy(comercio = "TRANSFERENCIA A VICENTE", pesos = 3_800,
                    nota = "Mi parte de la pizza"), compra.copy(nota = "Compra compartida de la feria"), compra)[temporada]
                else -> compra
            }
            val random = Random(base + i * 137L)
            val recurrente = detalle.categoria == SUBSCRIPTIONS_ID
            val dia = if (recurrente) detalle.dia else
                (detalle.dia + random.nextInt(-1, 2)).coerceIn(1, month.lengthOfMonth())
            val fecha = month.atDay(dia)
            if (fecha > today) return@mapIndexedNotNull null
            val monto = when {
                recurrente || detalle.comercio == "INTERNET HOGAR" -> detalle.pesos
                detalle.operacion == "Recarga" -> detalle.pesos + 500 * random.nextInt(-1, 2)
                else -> detalle.pesos + 50 * random.nextInt(-5, 9)
            }
            val comercio = when (detalle.comercio) {
                "SUPERMERCADO" -> listOf("SUPERMERCADO LIDER", "SUPERMERCADO JUMBO")[random.nextInt(2)]
                "CINE" -> if (month.monthValue % 2 == 0) "CINE DEL CENTRO" else "CINE PLAZA"
                else -> detalle.comercio
            }
            val nota = when {
                i == 2 && month.monthValue % 2 == 0 -> "Pan y queso para la once"
                i == 6 && month.monthValue % 3 == 0 -> "Tomates y papas para el almuerzo"
                else -> detalle.nota
            }
            Egreso(base + i + 1, fecha.toEpochDay(), comercio, detalle.operacion, monto.toLong(),
                if (offset == 0 && i < 2) null else detalle.categoria, nota)
        }
    }
}
