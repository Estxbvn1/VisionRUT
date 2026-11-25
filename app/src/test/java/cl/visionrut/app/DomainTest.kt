package cl.visionrut.app

import cl.visionrut.app.data.DemoData
import cl.visionrut.app.data.IncludedCategories
import cl.visionrut.app.data.Recordatorio
import cl.visionrut.app.domain.CalculadorResumen.distribution
import cl.visionrut.app.domain.CalculadorResumen.expensesInMonth
import cl.visionrut.app.domain.CalculadorRecurrencia.nextDue
import cl.visionrut.app.domain.CalculadorRecurrencia.nextOccurrence
import cl.visionrut.app.domain.noteLength
import cl.visionrut.app.domain.noteLimited
import cl.visionrut.app.domain.CalculadorResumen.projection
import cl.visionrut.app.ui.SystemBackAction
import cl.visionrut.app.ui.systemBackAction
import cl.visionrut.app.ui.screens.settledCategoryReveal
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class DomainTest {
    private val current = YearMonth.of(2026, 11)
    private val history = (0..3).flatMap { DemoData.month(current.minusMonths(it.toLong()), current.atEndOfMonth()) }
    private val classified = history.map { if (it.categoryId == null) it.copy(categoryId = 1L) else it }

    @Test
    fun demoMonthIsBalancedAndOnlySomeExpensesHaveNotes() {
        for (mes in 8..11) {
            val periodo = YearMonth.of(2026, mes)
            val total = DemoData.month(periodo, periodo.atEndOfMonth()).sumOf { it.amount }
            assertTrue("$periodo suma $total", total in 360_000L..420_000L)
        }
        val closed = (1L..3L).map { DemoData.month(current.minusMonths(it), current.atEndOfMonth()) }
        closed.forEach { rows ->
            assertTrue(rows.size in 19..35)
            assertTrue(rows.count { it.note != null } in (rows.size / 2 + 1) until rows.size)
            assertTrue(rows.all { noteLength(it.note.orEmpty()) <= 40 })
            assertEquals(rows.size, rows.distinctBy { it.id }.size)
            for (row in rows) {
                val delivery = row.description.contains("EATS", ignoreCase = true) ||
                    row.description.contains("RAPPI", ignoreCase = true)
                assertEquals(delivery, row.categoryId == null)
                if (delivery) assertFalse(row.note.isNullOrBlank())
            }
        }
        val date = current.atDay(15)
        val partial = DemoData.month(current, date)
        assertTrue(partial.all { it.day <= date.toEpochDay() })
        assertTrue(partial.size < DemoData.month(current, current.atEndOfMonth()).size)
    }

    @Test
    fun categorizingChangesDistributionAndPreservesTheBankTotal() {
        val rows = DemoData.month(current, current.atEndOfMonth())
        val pending = rows.filter { it.categoryId == null }
        val edited = rows.map { if (it.categoryId == null) it.copy(categoryId = 7L) else it }
        assertEquals(rows.sumOf { it.amount }, edited.sumOf { it.amount })
        val expectedPets = rows.filter { it.categoryId == 7L }.sumOf { it.amount } + pending.sumOf { it.amount }
        assertEquals(expectedPets, distribution(edited, IncludedCategories).first { it.category?.id == 7L }.amount)
        assertTrue(edited.none { it.categoryId == null })
    }

    @Test
    fun roundingByCategoryNeverChangesTheProjectedTotal() {
        val end = current.atEndOfMonth()
        val before = projection(classified, IncludedCategories, current, end)
        val window = classified.filter { it.day >= current.minusMonths(2).atDay(1).toEpochDay() }
        val expected = (window.sumOf { it.amount } + 1) / 3
        assertEquals(expected, before.values.sumOf { it.amount })
        val row = window.first { it.categoryId == 1L }
        val changed = classified.map { if (it.id == row.id) it.copy(categoryId = 7L) else it }
        val after = projection(changed, IncludedCategories, current, end)
        assertEquals(expected, after.values.sumOf { it.amount })
        assertTrue(after.values.first { it.category?.id == 7L }.amount > before.values.first { it.category?.id == 7L }.amount)
    }

    @Test
    fun projectionUsesPartialMonthAndKeepsUncategorizedExpenses() {
        val today = current.atDay(15)
        val sample = history.first()
        val rows = listOf(
            sample.copy(day = current.minusMonths(1).atEndOfMonth().toEpochDay(), amount = 9_000, categoryId = 1L),
            sample.copy(day = today.toEpochDay(), amount = 3_000, categoryId = null),
            sample.copy(day = today.plusDays(1).toEpochDay(), amount = 50_000, categoryId = 1L),
        )
        val forecast = projection(rows, IncludedCategories, current, today)
        assertEquals(listOf(current.minusMonths(1), current), forecast.months.map { it.month })
        assertEquals(3_000L, forecast.values.single { it.category == null }.amount)
        assertEquals(7_500L, forecast.values.sumOf { it.amount })
        assertFalse(projection(emptyList(), IncludedCategories, current, today).ready)
    }

    @Test
    fun noteLimitCountsCharactersAndSupportsClearing() {
        assertEquals(11, noteLength("Comida gato"))
        assertEquals(40, noteLength(noteLimited("🐱".repeat(41))))
        assertEquals("", noteLimited(""))
        assertEquals("a".repeat(40), noteLimited("a".repeat(45)))
    }

    @Test
    fun monthlyRecurrencePreservesDayAcrossShortMonths() {
        val anchor = LocalDate.of(2027, 1, 31)
        assertEquals(
            LocalDate.of(2027, 2, 28),
            nextOccurrence(anchor, "mensual", LocalDate.of(2027, 2, 1)),
        )
        assertEquals(
            LocalDate.of(2027, 3, 31),
            nextOccurrence(anchor, "mensual", LocalDate.of(2027, 3, 1)),
        )
        assertEquals(
            LocalDate.of(2028, 2, 29),
            nextOccurrence(anchor, "mensual", LocalDate.of(2028, 2, 1)),
        )
        assertEquals(anchor, nextOccurrence(anchor, "mensual", anchor))
    }

    @Test
    fun yearlyRecurrencePreservesLeapDayAndSkipsNotifiedDue() {
        val anchor = LocalDate.of(2024, 2, 29)
        assertEquals(
            LocalDate.of(2025, 2, 28),
            nextOccurrence(anchor, "anual", LocalDate.of(2025, 1, 1)),
        )
        assertEquals(
            LocalDate.of(2028, 2, 29),
            nextOccurrence(anchor, "anual", LocalDate.of(2028, 1, 1)),
        )
        val reminder =
            Recordatorio(
                expenseId = 1,
                service = "Prueba",
                anchorDay = LocalDate.of(2026, 10, 8).toEpochDay(),
                frequency = "mensual",
                lastNotifiedDue = LocalDate.of(2026, 11, 8).toEpochDay(),
            )
        assertEquals(LocalDate.of(2026, 12, 8), reminder.nextDue(LocalDate.of(2026, 11, 7)))
    }

    @Test
    fun demoPurchasesVaryButSubscriptionsKeepTheirCalendarDay() {
        val months = (1L..3L).map { DemoData.month(current.minusMonths(it), current.atEndOfMonth()) }
        assertTrue(months.map { rows -> rows.map { it.description } }.distinct().size > 1)
        for (rows in months) {
            val video = rows.single { it.description == DemoData.NETFLIX }
            val music = rows.single { it.description == DemoData.SPOTIFY }
            assertEquals(1, LocalDate.ofEpochDay(video.day).dayOfMonth)
            assertEquals(2, LocalDate.ofEpochDay(music.day).dayOfMonth)
        }
    }

    @Test
    fun systemBackClosesPanelsAndReturnsToSummaryBeforeLeavingTheModule() {
        assertEquals(SystemBackAction.SUMMARY, systemBackAction(true, 1, false))
        assertEquals(SystemBackAction.SUMMARY, systemBackAction(true, 2, false))
        assertEquals(SystemBackAction.BANK_HOME, systemBackAction(true, 0, false))
        assertEquals(SystemBackAction.SYSTEM, systemBackAction(false, 0, false))
        for (tab in 0..2) assertEquals(
            SystemBackAction.CLOSE_PANEL,
            systemBackAction(true, tab, true),
        )
    }

    @Test
    fun swipingSettlesToARevealedActionRatherThanDismissingTheRow() {
        assertEquals(-108f, settledCategoryReveal(-13f, 108f, 12f), 0f)
        assertEquals(-108f, settledCategoryReveal(-108f, 108f, 12f), 0f)
        assertEquals(0f, settledCategoryReveal(-5f, 108f, 12f), 0f)
        assertEquals(0f, settledCategoryReveal(0f, 108f, 12f), 0f)
    }
}
