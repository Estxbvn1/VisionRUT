package cl.visionrut.app.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

val Chile: Locale = Locale.forLanguageTag("es-CL")

fun money(value: Long): String = "$" + NumberFormat.getIntegerInstance(Chile).format(value)

fun monthLabel(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Chile)).replaceFirstChar {
        it.titlecase(Chile)
    }

fun dateLabel(day: Long): String =
    LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMM yyyy", Chile))
