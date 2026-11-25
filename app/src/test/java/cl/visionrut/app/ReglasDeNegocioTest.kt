package cl.visionrut.app

import cl.visionrut.app.data.Egreso
import cl.visionrut.app.data.IncludedCategories
import cl.visionrut.app.data.Recordatorio
import cl.visionrut.app.domain.CalculadorRecurrencia.billingDate
import cl.visionrut.app.domain.CalculadorRecurrencia.nextDue
import cl.visionrut.app.domain.CalculadorResumen.expensesInMonth
import cl.visionrut.app.domain.CalculadorResumen.projection
import cl.visionrut.app.domain.nombreCategoriaDuplicado
import cl.visionrut.app.domain.normalizarNombreCategoria
import cl.visionrut.app.domain.noteLength
import cl.visionrut.app.domain.noteLimited
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasDeNegocioTest {
    @Test
    fun filtroMensualSeparaLosAniosYConservaElOrdenPorFecha() {
        val enero = YearMonth.of(2027, 1)
        val rows = listOf(
            egreso(1, enero.minusMonths(1).atEndOfMonth(), 2_000),
            egreso(2, enero.atDay(1), 1_200),
            egreso(4, enero.atEndOfMonth(), 5_600),
            egreso(3, enero.atEndOfMonth(), 3_500),
            egreso(5, enero.plusMonths(1).atDay(1), 6_000),
            egreso(6, enero.minusYears(1).atDay(1), 5_000),
        )

        assertEquals(listOf(3L, 4L, 2L), expensesInMonth(rows, enero).map { it.id })
    }

    @Test
    fun proyeccionCombinaFebreroBisiestoParcialConLosDosMesesAnteriores() {
        val febrero = YearMonth.of(2028, 2)
        val hoy = febrero.atDay(14)
        val rows = listOf(
            egreso(1, febrero.minusMonths(3).atEndOfMonth(), 91_000),
            egreso(2, febrero.minusMonths(2).atEndOfMonth(), 27_400),
            egreso(3, febrero.minusMonths(1).atEndOfMonth(), 20_000),
            egreso(4, hoy, 14_000),
            egreso(5, hoy.plusDays(1), 80_000),
        )

        val forecast = projection(rows, IncludedCategories, febrero, hoy)

        assertEquals(listOf(febrero.minusMonths(2), febrero.minusMonths(1), febrero),
            forecast.months.map { it.month })
        assertEquals(14, forecast.months.last().observedDays)
        assertTrue(forecast.months.last().partial)
        // (27.400 + 20.000 + 14.000 / 14 × 29) / 3, redondeado a pesos.
        assertEquals(25_467L, forecast.values.sumOf { it.amount })
    }

    @Test
    fun notaConservaElEmojiCompletoEnElCaracterCuarenta() {
        val expected = "á".repeat(39) + "🐱"

        assertEquals(expected, noteLimited(expected + "x"))
        assertEquals(40, noteLength(expected))
    }

    @Test
    fun nombreDeCategoriaEliminaEspaciosYValidaLosLimites() {
        val maxLength = "a".repeat(30)

        assertEquals(maxLength, normalizarNombreCategoria("  ${maxLength}  "))
        assertThrows(IllegalArgumentException::class.java) { normalizarNombreCategoria("   ") }
        assertThrows(IllegalArgumentException::class.java) { normalizarNombreCategoria(maxLength + "x") }
    }

    @Test
    fun nombreDeCategoriaDetectaDuplicadosConMayusculasAcentuadas() {
        val existing = listOf("Alimentación", "Ñandú")

        assertTrue(nombreCategoriaDuplicado("  ALIMENTACIÓN  ", existing))
        assertTrue(nombreCategoriaDuplicado("ÑANDÚ", existing))
        assertFalse(nombreCategoriaDuplicado("Delivery", existing))
    }

    @Test
    fun avisoEntregadoNoAdelantaLaFechaDeCobroVisible() {
        val billingDay = LocalDate.of(2027, 1, 31)
        val reminder = Recordatorio(
            expenseId = 1,
            service = "Netflix",
            anchorDay = billingDay.toEpochDay(),
            frequency = "mensual",
            lastNotifiedDue = billingDay.toEpochDay(),
        )

        assertEquals(billingDay, reminder.billingDate(billingDay))
        assertEquals(LocalDate.of(2027, 2, 28), reminder.nextDue(billingDay))
    }

    private fun egreso(id: Long, date: LocalDate, amount: Long) =
        Egreso(
            id = id,
            day = date.toEpochDay(),
            description = "Compra de prueba",
            operation = "Compra con tarjeta",
            amount = amount,
            categoryId = 1L,
        )
}
