package cl.visionrut.app.domain

import cl.visionrut.app.data.Recordatorio
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Calcula las recurrencias desde la fecha original para conservar el día del cobro. */
object CalculadorRecurrencia {
    fun nextOccurrence(anchor: LocalDate, frequency: String, onOrAfter: LocalDate): LocalDate {
        if (!anchor.isBefore(onOrAfter)) return anchor
        val period =
            if (frequency == "anual") ChronoUnit.YEARS.between(anchor, onOrAfter)
            else ChronoUnit.MONTHS.between(YearMonth.from(anchor), YearMonth.from(onOrAfter))
        var n = period.coerceAtLeast(0)
        fun occurrence(): LocalDate =
            if (frequency == "anual") anchor.plusYears(n) else anchor.plusMonths(n)
        while (occurrence().isBefore(onOrAfter)) n++
        return occurrence()
    }

    fun Recordatorio.nextDue(today: LocalDate = LocalDate.now()): LocalDate =
        nextOccurrence(
            LocalDate.ofEpochDay(anchorDay),
            frequency,
            maxOf(today, LocalDate.ofEpochDay(lastNotifiedDue).plusDays(1)),
        )

    /** El aviso de la víspera no adelanta la fecha de cobro que ve el usuario. */
    fun Recordatorio.billingDate(today: LocalDate = LocalDate.now()): LocalDate =
        nextOccurrence(LocalDate.ofEpochDay(anchorDay), frequency, today)
}
