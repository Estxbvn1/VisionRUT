package cl.visionrut.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Ajusta la cartola precargada en v3 conservando sus identificadores y las ediciones personales. */
internal object Migracion3a4 : Migration(3, 4) {
    private data class Precio(val id: Long, val glosa: String, val anterior: Long, val nuevo: Long)

    private data class Reemplazo(
        val id: Long, val glosaAnterior: String, val montoAnterior: Long, val notaAnterior: String,
        val glosa: String, val monto: Long, val categoria: Long, val nota: String,
        val operacion: String = "Compra con tarjeta de débito",
    )

    private val precios = listOf(
        Precio(202_608_003, "LIDER EXPRESS STA ISABEL", 56_483, 46_928),
        Precio(202_608_005, "Servipag (www.servipag.com)", 32_461, 24_861),
        Precio(202_608_013, "MERPAGO*MERCADOLIBRE", 48_582, 36_482),
        Precio(202_608_018, "JUMBO ÑUÑOA", 29_427, 26_417),
        Precio(202_609_001, "SANTA ISABEL ÑUÑOA", 63_874, 58_934),
        Precio(202_609_005, "Servipag (www.servipag.com)", 29_683, 23_683),
        Precio(202_609_011, "DL*UBEREATS SANTIAGO CHL", 28_327, 22_167),
        Precio(202_609_013, "UNIMARC IRARRAZAVAL", 32_947, 41_837),
        Precio(202_610_003, "LIDER.CL", 61_732, 41_372),
        Precio(202_610_005, "SERVIPAG.COM", 27_418, 22_818),
        Precio(202_610_008, "MERPAGO*MERCADOLI Las Condes CHL", 52_452, 36_452),
        Precio(202_610_014, "UBER *TRIP", 7_642, 6_342),
        Precio(202_610_017, "UNIMARC STA ISABEL", 21_386, 17_864),
        Precio(202_610_018, "TEF A NICO R", 12_000, 7_000),
        Precio(202_610_024, "PET HAPPY", 17_684, 13_684),
        Precio(202_610_025, "JUMBO ÑUÑOA", 29_463, 22_563),
        Precio(202_610_026, "MERPAGO*NICHOSTORE", 24_690, 15_690),
        Precio(202_611_002, "LIDER EXPRESS STA ISABEL", 58_673, 46_373),
        Precio(202_611_008, "Servipag (www.servipag.com)", 25_861, 21_861),
        Precio(202_611_016, "MERCADOPAGO *MERCADOL", 43_741, 32_741),
        Precio(202_611_017, "SANTA ISABEL ÑUÑOA", 26_918, 21_438),
        Precio(202_611_026, "UNIMARC IRARRAZAVAL", 23_862, 21_276),
    )

    private val transferencias = listOf(
        Reemplazo(202_608_002, "TEF A P SOLEDAD V", 365_000, "Arriendo del departamento",
            "MERPAGO*ISAMORA", 7_350, 4, "Dos libros usados en la feria"),
        Reemplazo(202_608_011, "TEF A COMUNIDAD", 46_271, "Gastos comunes",
            "TBK COMERCIAL VARGAS", 9_874, 4, "Tendedero plegable para el baño"),
        Reemplazo(202_609_002, "TEF A P SOLEDAD V", 365_000, "Arriendo de septiembre",
            "TEF A DANIELA M", 9_500, 5, "Mi parte del regalo para Nico", "Transferencia enviada"),
        Reemplazo(202_609_009, "TEF A COMUNIDAD", 44_832, "Gastos comunes del edificio",
            "MERPAGO*SOFIAR", 8_370, 4, "Guirnalda y vasos para el asado"),
        Reemplazo(202_610_002, "TEF A P SOLEDAD V", 365_000, "Arriendo de octubre",
            "MERPAGO*LUCASG", 5_480, 1, "Marraquetas y queso de la panadería"),
        Reemplazo(202_610_011, "TEF A COMUNIDAD", 45_616, "Gastos comunes",
            "BCI PAGOS *M ROJAS", 7_480, 4, "Ajuste del pantalón en la sastrería"),
        Reemplazo(202_611_003, "TEF A P SOLEDAD V", 365_000, "Arriendo del depto.",
            "MERPAGO*PAULAM", 5_860, 1, "Café y tostadas después de la pega"),
        Reemplazo(202_611_014, "TEF A COMUNIDAD", 47_318, "Gastos comunes de octubre",
            "TBK BOWLING PLAZA", 11_480, 5, "Una partida de bowling con Nico"),
    )

    override fun migrate(db: SupportSQLiteDatabase) {
        for (p in precios) {
            db.execSQL("UPDATE egresos SET monto=? WHERE id=? AND descripcion=? AND monto=?",
                arrayOf(p.nuevo, p.id, p.glosa, p.anterior))
        }
        // Una transferencia ya anotada o reclasificada conserva también su significado original.
        for (r in transferencias) {
            db.execSQL("""UPDATE egresos SET descripcion=?, monto=?, categoria_id=?, nota=?, tipo_operacion=?
                WHERE id=? AND descripcion=? AND monto=? AND categoria_id=3 AND nota=?
                AND tipo_operacion='Transferencia enviada'
                AND NOT EXISTS (SELECT 1 FROM recordatorios WHERE egreso_id=egresos.id)""",
                arrayOf(r.glosa, r.monto, r.categoria, r.nota, r.operacion,
                    r.id, r.glosaAnterior, r.montoAnterior, r.notaAnterior))
        }
    }
}
