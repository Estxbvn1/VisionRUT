package cl.visionrut.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.visionrut.app.data.Egreso
import cl.visionrut.app.domain.CategoryTotal
import cl.visionrut.app.ui.components.Aviso
import cl.visionrut.app.ui.components.BotonPrincipal
import cl.visionrut.app.ui.components.FilaAccion
import cl.visionrut.app.ui.components.IconoCategoria
import cl.visionrut.app.ui.components.SelectorMes
import cl.visionrut.app.ui.components.Tarjeta
import cl.visionrut.app.ui.components.TituloPanel
import cl.visionrut.app.ui.theme.Alert
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
    val totals = data.categoryTotals
    val total = rows.sumOf { it.amount }
    LazyColumn(
        Modifier.fillMaxSize().testTag("summary-list"),
        state = scroll,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SelectorMes(month, data.months, onMonth) }
        // Total del mes y movimientos pendientes.
        item {
            Tarjeta {
                Spacer(Modifier.height(5.dp))
                TotalEgresos(rows, centered = true)
                HorizontalDivider(Modifier.padding(vertical = 15.dp), color = Line)
                Row(
                    Modifier.fillMaxWidth()
                        .testTag("pending")
                        .background(
                            if (pending.isEmpty()) Color(0xFFF0F8F2) else Color(0xFFFFF5D9),
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { openExpenses("none") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (pending.isEmpty())
                        Icon(
                            Icons.Outlined.CheckCircle,
                            null,
                            Modifier.size(30.dp),
                            tint = Color(0xFF29A357),
                        )
                    else IconoCategoria(null, 32)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                            if (pending.isEmpty()) "Todo categorizado" else "Por categorizar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            if (pending.isEmpty()) "Tus egresos están organizados"
                            else "${pending.size} egresos · ${money(pending.sumOf { it.amount })}",
                            fontSize = 13.sp,
                            color = Muted,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        null,
                        Modifier.size(19.dp),
                        tint = Blue,
                    )
                }
            }
        }
        item {
            Tarjeta {
                Text("En qué gastaste", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Conoce la distribución de tu mes",
                    fontSize = 13.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (rows.none { it.categoryId != null }) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(76.dp).background(Alert.copy(alpha = .18f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                null,
                                Modifier.size(42.dp),
                                tint = Color(0xFF9B6800),
                            )
                        }
                        Text(
                            "Categoriza tus egresos\npara ver su distribución.",
                            color = Muted,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 14.dp),
                        )
                        BotonPrincipal("Ver egresos", Modifier.width(180.dp)) {
                            openExpenses("all")
                        }
                    }
                } else {
                    Box(
                        Modifier.align(Alignment.CenterHorizontally)
                            .padding(vertical = 12.dp)
                            .size(170.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        GraficoEgresos(totals, Modifier.fillMaxSize())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.BarChart, null, Modifier.size(25.dp), tint = Blue)
                            Text(
                                "Tu mes",
                                fontSize = 13.sp,
                                color = Muted,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    totals.chunked(2).forEach { group ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            group.forEach { item ->
                                Column(
                                    Modifier.weight(1f)
                                        .background(Color(0xFFF7F8FC), RoundedCornerShape(12.dp))
                                        .clickable { openExpenses(item.category?.id?.toString() ?: "none") }
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconoCategoria(item.category, 25)
                                        Text(
                                            item.category?.name ?: "Sin categoría",
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            modifier = Modifier.padding(start = 7.dp),
                                        )
                                    }
                                    Row(
                                        Modifier.fillMaxWidth().padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            money(item.amount),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            "%.1f %%".format(Chile, if (total == 0L) 0.0 else item.amount * 100.0 / total),
                                            fontSize = 12.sp,
                                            color = Muted,
                                        )
                                    }
                                }
                            }
                            if (group.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        if (rows.any { it.categoryId != null }) item {
            BotonPrincipal("Ver mis egresos") { openExpenses("all") }
        }
        item {
            Tarjeta {
                FilaAccion(
                    "Gestionar categorías",
                    Icons.Outlined.Tune,
                    "Personaliza tus categorías",
                    categories,
                )
            }
        }
        item {
            val forecast = data.forecast
            Tarjeta {
                Column(Modifier.fillMaxWidth().testTag("action-Proyección de egresos")
                    .clickable(onClick = openProjection).padding(vertical = 4.dp)) {
                    Text("Proyección de egresos", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(monthLabel(month.plusMonths(1)), fontSize = 14.sp, color = Muted)
                    Text(
                        if (forecast.ready) money(forecast.values.sumOf { it.amount })
                        else "Información insuficiente para estimar",
                        modifier = Modifier.padding(vertical = 6.dp),
                        fontSize = if (forecast.ready) 28.sp else 16.sp,
                        color = Blue, fontWeight = FontWeight.Bold,
                    )
                    Text("Ver detalle del cálculo", fontSize = 14.sp, color = Blue)
                }
            }
        }
        item {
            Text(
                "CuentaRUT •••• 1234 · Datos simulados",
                fontSize = 12.sp,
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun TotalEgresos(
    expenses: List<Egreso>,
    label: String = "Egresos del mes",
    centered: Boolean = false,
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            label,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Muted,
        )
        Text(
            money(expenses.sumOf { it.amount }),
            fontSize = 30.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        Text(
            "${expenses.size} ${if (expenses.size == 1) "egreso" else "egresos"}",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = Muted,
        )
    }
}

@Composable
private fun GraficoEgresos(totals: List<CategoryTotal>, modifier: Modifier = Modifier) {
    val total = totals.sumOf { it.amount }.toFloat()
    Canvas(
        modifier
            .semantics { contentDescription = "Distribución de egresos por categoría" }
            .padding(7.dp)
    ) {
        val stroke = size.minDimension * .27f
        val inset = stroke / 2
        var start = -90f
        totals.forEach { item ->
            val sweep = if (total == 0f) 0f else item.amount / total * 360f
            drawArc(
                item.category?.let { Color(it.color) } ?: Pending,
                start,
                (sweep - 1f).coerceAtLeast(0f),
                false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke),
            )
            start += sweep
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
