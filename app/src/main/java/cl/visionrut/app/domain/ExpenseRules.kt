package cl.visionrut.app.domain

import cl.visionrut.app.data.Categoria
import cl.visionrut.app.data.Egreso
import java.time.LocalDate
import java.time.YearMonth

// Totales mensuales y distribución por categoría.
data class CategoryTotal(val category: Categoria?, val amount: Long, val count: Int)

data class ProjectionMonth(
    val month: YearMonth,
    val observedDays: Int,
    val amounts: Map<Long?, Long>,
) {
    val partial: Boolean get() = observedDays < month.lengthOfMonth()
}

data class Projection(
    val months: List<ProjectionMonth> = emptyList(),
    val values: List<CategoryTotal> = emptyList(),
) {
    val ready: Boolean get() = months.isNotEmpty()
}

object CalculadorResumen {
    fun expensesInMonth(expenses: List<Egreso>, month: YearMonth): List<Egreso> =
        expenses
            .filter {
                YearMonth.from(LocalDate.ofEpochDay(it.day)) == month
            }
            .sortedWith(compareByDescending<Egreso> { it.day }.thenBy { it.id })

    fun distribution(expenses: List<Egreso>, categories: List<Categoria>): List<CategoryTotal> {
        val groups = expenses.groupBy { it.categoryId }
        return (categories.map {
                CategoryTotal(
                    it,
                    groups[it.id].orEmpty().sumOf(Egreso::amount),
                    groups[it.id].orEmpty().size,
                )
            } +
                CategoryTotal(
                    null,
                    groups[null].orEmpty().sumOf(Egreso::amount),
                    groups[null].orEmpty().size,
                ))
            .filter { it.amount > 0 }
    }

    fun projection(
        expenses: List<Egreso>,
        categories: List<Categoria>,
        current: YearMonth,
        today: LocalDate = LocalDate.now(),
    ): Projection {
        if (current > YearMonth.from(today)) return Projection()
        val months = (2L downTo 0L).mapNotNull { offset ->
            val month = current.minusMonths(offset)
            val end = minOf(today, month.atEndOfMonth())
            val days = month.atDay(1).toEpochDay()..end.toEpochDay()
            val amounts = expenses.filter { it.day in days }.groupBy { it.categoryId }
                .mapValues { (_, rows) -> rows.sumOf { it.amount } }
            if (amounts.isEmpty()) null else ProjectionMonth(month, end.dayOfMonth, amounts)
        }
        val latest = months.lastOrNull() ?: return Projection()
        if (latest.month != current) return Projection()

        // Denominador común para extender el mes parcial y redondear una sola vez en pesos.
        val numerators = mutableMapOf<Long?, Long>()
        for (period in months) {
            val weight = if (period.partial) period.month.lengthOfMonth() else latest.observedDays
            for ((id, amount) in period.amounts) {
                numerators[id] = numerators.getOrDefault(id, 0L) + amount * weight
            }
        }
        val divisor = latest.observedDays.toLong() * months.size
        val total = (numerators.values.sum() + divisor / 2) / divisor
        val extraPesos = total - numerators.values.sumOf { it / divisor }
        val roundedUp = numerators.keys.sortedWith(
            compareByDescending<Long?> { numerators.getValue(it) % divisor }.thenBy { it ?: Long.MAX_VALUE }
        ).take(extraPesos.toInt()).toSet()
        val values = (categories + listOf<Categoria?>(null)).mapNotNull { category ->
            val numerator = numerators[category?.id] ?: return@mapNotNull null
            val amount = numerator / divisor + if (category?.id in roundedUp) 1 else 0
            CategoryTotal(category, amount, 0)
        }
        return Projection(months, values)
    }
}

// Límite de notas contando caracteres Unicode completos.
fun noteLimited(value: String): String =
    value.codePoints().toArray().take(40).toIntArray().let { String(it, 0, it.size) }

fun noteLength(value: String): Int = value.codePointCount(0, value.length)
