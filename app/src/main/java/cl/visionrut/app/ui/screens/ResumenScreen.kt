package cl.visionrut.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.visionrut.app.domain.CategoryTotal
import cl.visionrut.app.ui.components.Aviso
import cl.visionrut.app.ui.components.BotonPrincipal
import cl.visionrut.app.ui.components.FilaAccion
import cl.visionrut.app.ui.components.IconoCategoria
import cl.visionrut.app.ui.components.SelectorMes
import cl.visionrut.app.ui.components.Tarjeta
import cl.visionrut.app.ui.components.TituloPanel
import cl.visionrut.app.ui.theme.Blue
import cl.visionrut.app.ui.theme.Ink
import cl.visionrut.app.ui.theme.Line
import cl.visionrut.app.ui.theme.Muted
import cl.visionrut.app.ui.theme.Pending
import cl.visionrut.app.util.Chile
import cl.visionrut.app.util.money
import cl.visionrut.app.util.monthLabel
import cl.visionrut.app.viewmodel.VisionRUTUiState
import java.time.YearMonth
import kotlin.math.atan2
import kotlin.math.hypot

/** Resumen del mes, pendientes y distribución por categorías. */
@Composable
fun ResumenScreen(
    data: VisionRUTUiState,
    month: YearMonth,
    scroll: LazyListState,
    onMonth: (YearMonth) -> Unit,
    openExpenses: (String) -> Unit,
    categories: () -> Unit,
    openProjection: () -> Unit,
) {
    val rows = data.monthlyExpenses
    val pending = rows.filter { it.categoryId == null }
    val total = rows.sumOf { it.amount }
    val totals = remember(data.categoryTotals) { data.categoryTotals.sortedByDescending { it.amount } }
    var chosenKey by rememberSaveable(month.toString()) { mutableStateOf<String?>(null) }
    val active = totals.firstOrNull { it.filterKey == chosenKey } ?: totals.firstOrNull()
    val hasDistribution = rows.any { it.categoryId != null }

    LazyColumn(
        Modifier.fillMaxSize().testTag("summary-list"),
        state = scroll,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SelectorMes(month, data.months, onMonth) }
        item {
            Tarjeta {
                Text("Egresos del mes", color = Muted, fontSize = 15.sp)
                Text(money(total), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("${rows.size} ${if (rows.size == 1) "egreso" else "egresos"}", color = Muted, fontSize = 14.sp)
                if (rows.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    FilaPendientes(pending.size, pending.sumOf { it.amount }) { openExpenses("none") }
                }
            }
        }
        item {
            Tarjeta {
                Row(
                    Modifier.fillMaxWidth().testTag("action-Proyección de egresos")
                        .clickable(onClick = openProjection).heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.TrendingUp, null, tint = Blue)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text("Estimación para ${monthLabel(month.plusMonths(1)).lowercase(Chile)}", color = Ink, fontSize = 15.sp)
                        Text(
                            if (data.forecast.ready) money(data.forecast.values.sumOf { it.amount })
                            else "Información insuficiente para estimar",
                            color = if (data.forecast.ready) Blue else Muted,
                            fontSize = if (data.forecast.ready) 19.sp else 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Ver cálculo", tint = Blue)
                }
            }
        }
        item {
            Tarjeta {
                Text("¿En qué gastaste?", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                if (hasDistribution && active != null) {
                    GraficoEgresos(totals, total, active, { chosenKey = it.filterKey }, Modifier.align(Alignment.CenterHorizontally))
                    if (totals.size > 1) {
                        Text("Toca un color para ver su porcentaje",
                            Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
                            color = Muted, fontSize = 13.sp)
                    }
                    totals.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = Line)
                        Row(
                            Modifier.fillMaxWidth().clickable { openExpenses(item.filterKey) }
                                .heightIn(min = 48.dp).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconoCategoria(item.category, 28)
                            Text(item.name, Modifier.weight(1f).padding(horizontal = 8.dp), fontSize = 14.sp)
                            Text(money(item.amount), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(item.percentage(total), Modifier.widthIn(min = 65.dp).padding(start = 4.dp),
                                color = Muted, fontSize = 13.sp, textAlign = TextAlign.End)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(18.dp), tint = Blue)
                        }
                    }
                } else {
                    Text(
                        if (rows.isEmpty()) "Este mes no tiene egresos." else "Categoriza tus egresos\npara ver su distribución.",
                        Modifier.padding(vertical = 16.dp), color = Muted, fontSize = 15.sp,
                    )
                }
                BotonPrincipal(if (hasDistribution) "Ver mis egresos" else "Ver egresos") { openExpenses("all") }
            }
        }
        item {
            Tarjeta { FilaAccion("Gestionar categorías", Icons.Outlined.Tune, onClick = categories) }
        }
        item {
            Text("CuentaRUT •••• 1234 · Datos simulados", Modifier.fillMaxWidth(),
                fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun FilaPendientes(count: Int, amount: Long, open: () -> Unit) {
    val complete = count == 0
    Row(
        Modifier.fillMaxWidth().testTag("pending")
            .background(if (complete) Color(0xFFF0F8F2) else Color(0xFFFFF5D9), RoundedCornerShape(10.dp))
            .clickable(enabled = !complete, onClick = open).heightIn(min = 48.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (complete) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Outlined.Label,
            null, Modifier.size(18.dp), tint = if (complete) Color(0xFF28723D) else Color(0xFF916100))
        Text(if (complete) "Todo categorizado" else "Por categorizar",
            Modifier.weight(1f).padding(horizontal = 8.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        if (!complete) {
            Text("$count ${if (count == 1) "egreso" else "egresos"} · ${money(amount)}", color = Ink, fontSize = 13.sp)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(18.dp), tint = Blue)
        }
    }
}

private val CategoryTotal.filterKey get() = category?.id?.toString() ?: "none"
private val CategoryTotal.name get() = category?.name ?: "Sin categoría"
private fun CategoryTotal.percentage(total: Long) = "%.1f %%".format(Chile, amount * 100.0 / total)

@Composable
private fun GraficoEgresos(
    totals: List<CategoryTotal>,
    total: Long,
    active: CategoryTotal,
    select: (CategoryTotal) -> Unit,
    modifier: Modifier = Modifier,
) {
    val share = active.percentage(total)
    val selectCategory by rememberUpdatedState(select)
    val label = if (active == totals.first()) "Mayor gasto" else "Categoría"
    Box(
        modifier.padding(vertical = 12.dp).size(188.dp).testTag("summary-chart").semantics(mergeDescendants = true) {
            contentDescription = "Distribución por categoría. ${active.name}, $share"
            onClick("Ver la siguiente categoría") {
                select(totals[(totals.indexOf(active) + 1) % totals.size])
                true
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().pointerInput(totals) {
            detectTapGestures { tap ->
                val dx = tap.x - size.width / 2f
                val dy = tap.y - size.height / 2f
                val radius = minOf(size.width, size.height) / 2f
                if (hypot(dx, dy) !in radius * .70f..radius) return@detectTapGestures
                // El primer segmento empieza a las doce, igual que el dibujo del anillo.
                val degrees = (Math.toDegrees(atan2(dy, dx).toDouble()) + 450.0) % 360.0
                val target = total * degrees / 360.0
                var accumulated = 0L
                for (item in totals) {
                    accumulated += item.amount
                    if (accumulated > target) {
                        selectCategory(item)
                        break
                    }
                }
            }
        }) {
            val stroke = size.minDimension * .15f
            val inset = stroke / 2
            var start = -90f
            totals.forEach { item ->
                val sweep = (item.amount * 360.0 / total).toFloat()
                drawArc(
                    item.category?.let { Color(it.color) } ?: Pending,
                    start, sweep - minOf(1f, sweep * .12f), false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(if (item == active) stroke else stroke * .78f),
                )
                start += sweep
            }
        }
        Column(Modifier.width(118.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Muted, fontSize = 12.sp)
            Text(active.name, fontSize = 15.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(share, fontSize = 20.sp, color = Blue, fontWeight = FontWeight.Bold)
        }
    }
}

/** Muestra qué meses y montos aportan a la estimación de cada categoría. */
@Composable
fun ProyeccionPanel(data: VisionRUTUiState, close: () -> Unit) {
    val current = data.selectedMonth
    val forecast = data.forecast
    var expanded by rememberSaveable(current.toString()) { mutableStateOf<Long?>(null) }
    TituloPanel("Proyección de egresos", close, monthLabel(current.plusMonths(1)))
    if (!forecast.ready) {
        Aviso("Aún no hay egresos en el mes seleccionado para calcular una proyección.", warning = true)
    } else {
        Text(
            "Meses usados: ${forecast.months.joinToString(" · ") { monthLabel(it.month) }}",
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        val latest = forecast.months.last()
        Aviso(if (latest.partial)
            "Estimamos el cierre con el gasto de los primeros ${latest.observedDays} días de ${monthLabel(current)}. El resultado se promedia con los meses anteriores disponibles."
        else "La estimación es el promedio de los totales mensuales disponibles.")
        Spacer(Modifier.height(10.dp))
        forecast.values.forEach { total ->
            val category = total.category
            val rowKey = category?.id ?: 0L
            Row(
                Modifier.fillMaxWidth()
                    .clickable { expanded = if (expanded == rowKey) null else rowKey }
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconoCategoria(category, 34)
                Text(
                    category?.name ?: "Sin categoría",
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Text(
                    money(total.amount),
                    fontSize = 16.sp,
                    color = Blue,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp), tint = Blue)
            }
            if (expanded == rowKey) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Color(0xFFEDF1FF), RoundedCornerShape(8.dp))
                        .padding(11.dp)
                ) {
                    forecast.months.forEach { period ->
                        val cutoff = if (period.partial) " al día ${period.observedDays}" else ""
                        Text(
                            "${monthLabel(period.month)}$cutoff: ${money(period.amounts[category?.id] ?: 0L)}",
                            fontSize = 14.sp,
                            color = Blue,
                        )
                    }
                    Text(
                        "Estimación: ${money(total.amount)}",
                        fontWeight = FontWeight.Bold,
                        color = Blue,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Line)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total estimado", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                money(forecast.values.sumOf { it.amount }),
                fontWeight = FontWeight.Bold,
                color = Blue,
                fontSize = 20.sp,
            )
        }
    }
    Text(
        "Es una referencia basada en tu historial. No representa un saldo disponible ni garantiza cargos futuros.",
        fontSize = 14.sp,
        color = Muted,
        modifier = Modifier.padding(top = 15.dp),
    )
}
