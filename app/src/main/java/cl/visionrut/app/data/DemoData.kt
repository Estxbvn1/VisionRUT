package cl.visionrut.app.data

import androidx.room.withTransaction
import java.time.LocalDate
import java.time.YearMonth

const val SUBSCRIPTIONS_ID = 6L

val IncludedCategories = listOf(
    Categoria(1, "Alimentación", "food", 0xFFFF7900, true),
    Categoria(2, "Transporte", "transport", 0xFF1685FF, true),
    Categoria(3, "Cuentas", "bills", 0xFF8850F6, true),
    Categoria(4, "Compras", "shopping", 0xFF00B8C4, true),
    Categoria(5, "Entretención", "fun", 0xFF9B36FF, true),
    Categoria(SUBSCRIPTIONS_ID, "Suscripciones", "subscriptions", 0xFFFF2BA8, true),
    Categoria(7, "Mascotas", "pets", 0xFFFFB600, true),
    Categoria(8, "Otros", "other", 0xFF98A2B7, true),
)

/** Cartola académica de agosto a noviembre de 2026. Los importes están expresados en CLP. */
object DemoData {
    val primerMes: YearMonth = YearMonth.of(2026, 8)
    val ultimoMes: YearMonth = YearMonth.of(2026, 11)
    fun mesDeReferencia(hoy: LocalDate = LocalDate.now()): YearMonth =
        minOf(YearMonth.from(hoy), ultimoMes)

    internal const val NETFLIX = "Netflix.com 866-579-7172"
    internal const val SPOTIFY = "Spotify P48B7B82B3"
    private const val DEBITO = "Compra con tarjeta de débito"
    private const val DIGITAL = "Compra tarjeta digital"
    private const val SERVICIO = "Pago de cuentas"
    private const val TRANSFERENCIA = "Transferencia enviada"
    private const val RECURRENTE = "Cobro recurrente"

    // La glosa pertenece a la cartola. La categoría y la nota pertenecen a la persona.
    private data class Cargo(
        val dia: Int, val glosa: String, val pesos: Long, val categoria: Long?,
        val nota: String? = null, val operacion: String = DEBITO,
    )

    private val agosto = listOf(
        Cargo(2, "MERPAGO*RAMONDIAZ", 4_730, 1, "papas y zanahorias"),
        Cargo(3, "MERPAGO*ISAMORA", 7_350, 4, "2 libros de la feria"),
        Cargo(4, "LIDER EXPRESS STA ISABEL", 46_928, 1, "pollo pa congelar y mercadería"),
        Cargo(5, "BIP! CARGA WEB", 15_000, 2, operacion = DIGITAL),
        Cargo(6, "Servipag (www.servipag.com)", 24_861, 3, "Enel julio", SERVICIO),
        Cargo(7, "ALMACEN JUANITA", 8_630, 1, "huevos, queso y pan amasado"),
        Cargo(8, "PAYU *UBER TRIP SANTIAGO CHL", 11_525, 2, "uber de vuelta"),
        Cargo(9, "DL RAPPI CHILE RAPPI", 14_580, null, "Thai Express, pad thai", DIGITAL),
        Cargo(11, "PAC AGUAS ANDINAS", 13_784, 3, operacion = SERVICIO),
        Cargo(12, "TBK COMERCIAL VARGAS", 9_874, 4, "tendedero chico"),
        Cargo(13, "ALVI CLUB MAYORISTA", 23_756, 1),
        Cargo(14, "MERPAGO*MERCADOLIBRE", 36_482, 4, "zapatillas pa trotar", DIGITAL),
        Cargo(15, "TBK CINEPLANET", 12_800, 5, "2 entradas al cine"),
        Cargo(16, "MERPAGO*LEONORV", 6_850, 1),
        Cargo(18, "SERVIPAG.COM", 18_990, 3, "Mundo Pacífico", SERVICIO),
        Cargo(20, "PASAJE QR RED", 815, 2, "metro con QR"),
        Cargo(21, "JUMBO ÑUÑOA", 26_417, 1),
        Cargo(22, "TEF A NICO R", 10_000, 5, "mi parte del pool", TRANSFERENCIA),
        Cargo(23, "DL*UBEREATS SANTIAGO CHL", 17_486, null, "Los Insaciables, una familiar", DIGITAL),
        Cargo(25, "CRUZ VERDE", 7_649, 4),
        Cargo(26, "PET HAPPY", 14_569, 7, "2 kg de pellet"),
        Cargo(28, "UNIMARC IRARRAZAVAL", 18_437, 1),
        Cargo(30, "PANADERIA EL TRIGAL", 3_260, 1),
    )

    private val septiembre = listOf(
        Cargo(2, "SANTA ISABEL ÑUÑOA", 58_934, 1, "súper del mes"),
        Cargo(3, "TEF A DANIELA M", 9_500, 5, "cuota regalo cumple", TRANSFERENCIA),
        Cargo(4, "MERPAGO*JUANITASPA", 7_845, 1, "pan, yogur y 2 paltas"),
        Cargo(5, "BIP! CARGA WEB", 20_000, 2, "carga bip pa la pega", DIGITAL),
        Cargo(7, "Servipag (www.servipag.com)", 23_683, 3, "Enel agosto", SERVICIO),
        Cargo(8, "MERPAGO*ALIPAYSINGAPOREC", 17_693, 4, "soporte del celu pa la bici", DIGITAL),
        Cargo(9, "PAC AGUAS ANDINAS", 12_967, 3, operacion = SERVICIO),
        Cargo(10, "MERPAGO*SOFIAR", 8_370, 4, "vasos y banderitas pal 18"),
        Cargo(11, "UBER RIDES", 6_873, 2, "ida a la junta"),
        Cargo(13, "DL*UBEREATS SANTIAGO CHL", 22_167, null, "Niu Sushi, promo de rolls", DIGITAL),
        Cargo(15, "SERVIPAG.COM", 18_990, 3, "Mundo Pacífico, fibra", SERVICIO),
        Cargo(16, "UNIMARC IRARRAZAVAL", 41_837, 1, "carne y pebre pal 18"),
        Cargo(17, "COPEC SAN BERNARDO", 24_387, 2, "93 pa la ida al sur"),
        Cargo(18, "MERPAGO*MARISOLP", 9_680, 1, "empanadas de pino"),
        Cargo(19, "TBK SOC RECREATIVA", 8_500, 5, "entrada fonda"),
        Cargo(21, "PAGO EN LINEA AUTOPISTAS", 13_200, 2, "TAG Survías, Talca-Chillán", SERVICIO),
        Cargo(23, "LIDER EXPRESS STA ISABEL", 27_458, 1),
        Cargo(24, "ALMACEN JUANITA", 4_370, 1),
        Cargo(26, "MERPAGO*PATRICIOS", 8_750, 7, "arena gato 5 kg"),
        Cargo(28, "PASAJE QR RED", 815, 2, "metro domingo, QR"),
        Cargo(30, "JUMBO ÑUÑOA", 16_394, 1),
    )

    private val octubre = listOf(
        Cargo(2, "MERPAGO*RAMONDIAZ", 6_380, 1, "frutillas y plátanos"),
        Cargo(3, "MERPAGO*LUCASG", 5_480, 1, "marraquetas y queso"),
        Cargo(4, "LIDER.CL", 41_372, 1, "pedido del Líder, retiro", DIGITAL),
        Cargo(5, "PASAJE QR RED", 815, 2, "metro de vuelta"),
        Cargo(6, "SERVIPAG.COM", 22_818, 3, "luz septiembre", SERVICIO),
        Cargo(6, "PAYU *UBER EATS", 12_632, null, "El Árabe, falafel", DIGITAL),
        Cargo(7, "MERPAGO*MERCADOLI Las Condes CHL", 36_452, 4, "perfume pa regalo", DIGITAL),
        Cargo(8, "BIP! CARGA WEB", 15_000, 2, operacion = DIGITAL),
        Cargo(9, "AGUAS ANDINAS WEB", 13_286, 3, operacion = SERVICIO),
        Cargo(10, "BCI PAGOS *M ROJAS", 7_480, 4, "basta del pantalón"),
        Cargo(11, "DL RAPPI CHILE RAPPI", 15_874, null, "La Burguesía, promo", DIGITAL),
        Cargo(12, "BCI PAGOS *J PARRA", 8_465, 1, "pan y jamón pa la once"),
        Cargo(13, "UBER *TRIP", 6_342, 2, "uber pa la casa"),
        Cargo(14, "Merpago*mcdonalds las condes", 13_398, 1, "2 combos del Mc"),
        Cargo(15, "SERVIPAG.COM", 18_990, 3, "Mundo Pacífico", SERVICIO),
        Cargo(16, "UNIMARC STA ISABEL", 17_864, 1),
        Cargo(17, "TEF A NICO R", 7_000, 5, "cuota pichanga", TRANSFERENCIA),
        Cargo(18, "CINEMARK PLAZA", 8_950, 5),
        Cargo(19, "PAYU *UBER TRIP SANTIAGO CHL", 12_797, 2, "vuelta del cumple"),
        Cargo(20, "MERPAGO*CAROLINAF", 4_690, 1),
        Cargo(21, "DL*UBEREATS SANTIAGO CHL", 19_346, null, "Momotaro, 2 ramen", DIGITAL),
        Cargo(22, "PET HAPPY", 13_684, 7, "pellet y ratón de juguete"),
        Cargo(23, "JUMBO ÑUÑOA", 22_563, 1),
        Cargo(24, "MERPAGO*NICHOSTORE", 15_690, 4, "polera pa la pega", DIGITAL),
        Cargo(25, "PASAJE QR RED", 815, 2),
        Cargo(26, "ALMACEN JUANITA", 7_483, 1),
        Cargo(28, "RAPPI CHILE", 11_937, null, "Ávila, arepa de pollo", DIGITAL),
        Cargo(29, "PANADERIA EL TRIGAL", 3_846, 1),
        Cargo(31, "TEF A DANIELA M", 9_500, 5, "mitad de los schop", TRANSFERENCIA),
    )

    private val noviembre = listOf(
        Cargo(1, "MERPAGO*GONZALEZR", 5_740, null),
        Cargo(2, "LIDER EXPRESS STA ISABEL", 46_373, 1, "arroz, porotos y detergente"),
        Cargo(3, "MERPAGO*PAULAM", 5_860, 1, "cortado y tostadas"),
        Cargo(3, "BIP! CARGA WEB", 10_000, 2, operacion = DIGITAL),
        Cargo(4, "ALMACEN JUANITA", 8_630, 1, "pan, queso y huevos"),
        Cargo(5, "Payu *uber eats", 16_960, null, "Mexa, burrito", DIGITAL),
        Cargo(6, "Servipag (www.servipag.com)", 21_861, 3, "Enel octubre", SERVICIO),
        Cargo(7, "Uber rides", 9_299, 2, "vuelta del carrete"),
        Cargo(8, "MERPAGO*RAMONDIAZ", 5_480, 1, "tomate y palta"),
        Cargo(9, "PAC AGUAS ANDINAS", 14_352, 3, operacion = SERVICIO),
        Cargo(10, "Merpago*mcdonalds", 13_314, 1, "almuerzo en el Mc"),
        Cargo(10, "POS COMPRA DEBITO", 7_283, null),
        Cargo(11, "TBK FUENTE DE SODA", 11_480, 5, "dos schop y papas pa picar"),
        Cargo(12, "PASAJE QR RED", 895, 2, "metro punta, QR"),
        Cargo(13, "MERCADOPAGO *MERCADOL", 32_741, 4, "zapatillas pa la pega", DIGITAL),
        Cargo(14, "SANTA ISABEL ÑUÑOA", 21_438, 1),
        Cargo(15, "DL RAPPI CHILE RAPPI", 18_435, null, "Da Dino, pizza familiar", DIGITAL),
        Cargo(16, "SERVIPAG.COM", 18_990, 3, "internet Mundo Pacífico", SERVICIO),
        Cargo(17, "MERPAGO*VENTAS JF", 6_387, null),
        Cargo(18, "PAYU *UBER TRIP SANTIAGO CHL", 6_817, 2, "uber al dentista"),
        Cargo(20, "COPEC IRARRAZAVAL", 18_924, 2, "12 L de 93"),
        Cargo(21, "TEF A NICO R", 15_000, 5, "cuota pal asado", TRANSFERENCIA),
        Cargo(23, "PET HAPPY", 16_439, 7),
        Cargo(25, "UNIMARC IRARRAZAVAL", 21_276, 1, "leche, fideos y atún"),
        Cargo(27, "TBK CINEPLANET", 9_780, 5),
        Cargo(29, "MERPAGO*JUANITASPA", 5_174, 1),
        // Se añaden al final para conservar los identificadores de los movimientos anteriores.
        Cargo(9, "MERPAGO*ISA VERA", 8_640, 4, "libro usado de la feria"),
        Cargo(16, "PET HAPPY IRARRAZAVAL", 7_860, 7, "5k arena gato"),
    )

    private fun baseId(month: YearMonth) = month.year * 100000L + month.monthValue * 1000L

    internal val respaldoAnual: Egreso = YearMonth.of(2026, 10).let { mes ->
        Egreso(baseId(mes) + 80, mes.atDay(12).toEpochDay(), "GOOGLE *Google One", RECURRENTE,
            14_990, SUBSCRIPTIONS_ID, "espacio pa las fotos")
    }

    fun month(month: YearMonth, today: LocalDate = LocalDate.now()): List<Egreso> {
        if (month !in primerMes..ultimoMes) return emptyList()
        val compras = when (month.monthValue) {
            8 -> agosto
            9 -> septiembre
            10 -> octubre
            else -> noviembre
        }
        val base = baseId(month)
        fun Cargo.egreso(clave: Int) = Egreso(base + clave, month.atDay(dia).toEpochDay(),
            glosa, operacion, pesos, categoria, nota)

        // Las claves 6 y 21 identifican las suscripciones desde la primera versión de la app.
        return buildList {
            var clave = 1
            for (cargo in compras) {
                if (clave == 6 || clave == 21) clave++
                add(cargo.egreso(clave++))
            }
            add(Cargo(1, NETFLIX, 7_190, SUBSCRIPTIONS_ID,
                if (month.monthValue == 11) "plan básico" else null, RECURRENTE).egreso(6))
            add(Cargo(2, SPOTIFY, 4_950, SUBSCRIPTIONS_ID,
                if (month.monthValue == 8 || month.monthValue == 10) "premium del mes" else null, RECURRENTE).egreso(21))
            if (month.monthValue == 10) add(respaldoAnual)
        }.filter { it.day <= today.toEpochDay() }
    }

    suspend fun ensure(db: VisionDatabase, today: LocalDate = LocalDate.now()): Unit =
        db.withTransaction {
            val dao = db.dao()
            val firstLoad = !dao.hasExpenses()
            val mes = mesDeReferencia(today)
            val nuevoRespaldo = respaldoAnual.day <= today.toEpochDay() && dao.expense(respaldoAnual.id) == null
            dao.insertCategories(IncludedCategories)
            for (numero in primerMes.monthValue..ultimoMes.monthValue) {
                dao.insertExpenses(month(YearMonth.of(primerMes.year, numero), today))
            }
            if (firstLoad && mes >= primerMes) {
                dao.saveReminder(Recordatorio(expenseId = baseId(mes) + 6, service = "Netflix",
                    anchorDay = mes.plusMonths(1).atDay(1).toEpochDay(), frequency = "mensual"))
            }
            if (nuevoRespaldo && dao.allReminders().none { it.service == "Software anual" }) {
                dao.saveReminder(Recordatorio(expenseId = respaldoAnual.id, service = "Respaldo en la nube",
                    anchorDay = LocalDate.ofEpochDay(respaldoAnual.day).plusYears(1).toEpochDay(),
                    frequency = "anual", enabled = false))
            }
        }
}
