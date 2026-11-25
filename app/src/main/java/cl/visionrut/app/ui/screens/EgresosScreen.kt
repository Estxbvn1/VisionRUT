package cl.visionrut.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.visionrut.app.data.SUBSCRIPTIONS_ID
import cl.visionrut.app.ui.ContenidoPanel
import cl.visionrut.app.data.Categoria
import cl.visionrut.app.data.Egreso
import cl.visionrut.app.domain.noteLength
import cl.visionrut.app.domain.noteLimited
import cl.visionrut.app.ui.components.BotonPrincipal
import cl.visionrut.app.ui.components.EgresoRow
import cl.visionrut.app.ui.components.IconoCategoria
import cl.visionrut.app.ui.components.SelectorMes
import cl.visionrut.app.ui.components.Tarjeta
import cl.visionrut.app.ui.components.TituloPanel
import cl.visionrut.app.ui.theme.Alert
import cl.visionrut.app.ui.theme.Blue
import cl.visionrut.app.ui.theme.Line
import cl.visionrut.app.ui.theme.Muted
import cl.visionrut.app.ui.theme.Red
import cl.visionrut.app.util.dateLabel
import cl.visionrut.app.util.money
import cl.visionrut.app.viewmodel.VisionRUTUiState
import java.time.YearMonth
import kotlin.math.roundToInt

/** Listado de egresos con filtros y selección múltiple. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EgresosScreen(
    data: VisionRUTUiState,
    month: YearMonth,
    filter: String,
    scroll: LazyListState,
    selecting: Boolean,
    selected: Set<Long>,
    onMonth: (YearMonth) -> Unit,
    onFilter: (String) -> Unit,
    clearFilter: () -> Unit,
    onLongPress: (Egreso) -> Unit,
    onClearCategory: (Egreso) -> Unit,
    onExpense: (Egreso) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val rows = data.monthlyExpenses
    val filtered = data.filteredExpenses
    LazyColumn(
        Modifier.fillMaxSize().testTag("expense-list"),
        state = scroll,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        item { SelectorMes(month, data.months, onMonth) }
        item {
            Tarjeta {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Egresos del mes",
                        fontSize = 14.sp,
                        color = Muted,
                        modifier = Modifier.weight(1f),
                    )
                    FiltroEgresos(filter, data.categories, rows.count { it.categoryId == null }, onFilter)
                }
                Text(
                    money(rows.sumOf { it.amount }),
                    fontSize = 30.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("${rows.size} egresos", fontSize = 13.sp, color = Muted)
            }
        }
        if (filter != "all") {
            item {
                Text(
                    "Total del filtro: ${money(filtered.sumOf { it.amount })} · ${filtered.size} egresos",
                    fontSize = 14.sp,
                    color = Blue,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                )
            }
        }
        if (filtered.isEmpty()) {
            item {
                Tarjeta {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            if (filter == "none") Icons.Outlined.TaskAlt
                            else Icons.AutoMirrored.Outlined.ReceiptLong,
                            null,
                            Modifier.size(62.dp),
                            tint = Color(0xFFAAB6D7),
                        )
                        Text(
                            when {
                                filter == "none" -> "No tienes egresos por categorizar\neste mes."
                                else ->
                                    "No hay egresos en esta categoría\npara el mes seleccionado."
                            },
                            color = Muted,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 17.dp),
                        )
                        BotonPrincipal("Ver todos", Modifier.width(180.dp), onClick = clearFilter)
                    }
                }
            }
        }
        // Cada fila admite edición, selección o descubrimiento de la acción.
        items(filtered, key = { it.id }) { expense ->
            val category = data.categories.find { it.id == expense.categoryId }
            val longPress: () -> Unit = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPress(expense)
            }
            if (category != null && !selecting) {
                EgresoDeslizable(
                    expense,
                    category,
                    longPress,
                    { onClearCategory(expense) },
                    { onExpense(expense) },
                )
            } else
                EgresoRow(
                    expense,
                    category,
                    selecting,
                    expense.id in selected,
                    onLongPress = longPress,
                ) {
                    onExpense(expense)
                }
        }
    }
}

@Composable
private fun FiltroEgresos(
    filter: String,
    categories: List<Categoria>,
    pendingCount: Int,
    choose: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.widthIn(max = 174.dp)) {
        Row(
            Modifier.testTag("filter-picker")
                .border(1.dp, Blue.copy(alpha = .18f), RoundedCornerShape(24.dp))
                .background(Color(0xFFF4F5FC), RoundedCornerShape(24.dp))
                .clickable { expanded = true }
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when (filter) {
                    "all" -> "Todos"
                    "none" -> "Sin categoría ($pendingCount)"
                    else -> categories.find { it.id.toString() == filter }?.name ?: "Todos"
                },
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                color = Blue,
            )
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                "Filtrar egresos",
                Modifier.padding(start = 4.dp).size(17.dp),
                tint = Blue,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 190.dp, max = 264.dp).heightIn(max = 320.dp),
            containerColor = Color.White,
        ) {
            val options = listOf("all" to "Todos", "none" to "Sin categoría ($pendingCount)") +
                categories.map { it.id.toString() to it.name }
            options.forEach { (id, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    modifier = Modifier.testTag("filter-choice-$id"),
                    trailingIcon = {
                        if (filter == id) Icon(Icons.Outlined.Check, "Seleccionado", tint = Blue)
                    },
                    onClick = {
                        expanded = false
                        choose(id)
                    },
                )
            }
        }
    }
}

/** El gesto descubre una acción; el cambio se guarda únicamente al tocarla. */
@Composable
private fun EgresoDeslizable(
    expense: Egreso,
    category: Categoria,
    longPress: (() -> Unit)?,
    clear: () -> Unit,
    open: () -> Unit,
) {
    val width = with(LocalDensity.current) { 108.dp.toPx() }
    val threshold = with(LocalDensity.current) { 12.dp.toPx() }
    var offset by remember(expense.id) { mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
        if (offset < 0f)
            Row(Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
                Column(
                    Modifier.fillMaxHeight()
                        .width(108.dp)
                        .background(Alert.copy(alpha = .22f))
                        .testTag("reveal-uncategorize-${expense.id}")
                        .clickable(enabled = offset < 0f, onClickLabel = "Dejar sin categoría") {
                            offset = 0f
                            clear()
                        }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        null,
                        Modifier.size(22.dp),
                        tint = Color(0xFF9B6800),
                    )
                    Text(
                        "Sin categoría",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
        Box(
            Modifier.offset { IntOffset(offset.roundToInt(), 0) }
                .draggable(
                    rememberDraggableState { delta ->
                        offset = (offset + delta).coerceIn(-width, 0f)
                    },
                    Orientation.Horizontal,
                    onDragStopped = { offset = settledCategoryReveal(offset, width, threshold) },
                )
        ) {
            EgresoRow(
                expense,
                category,
                onLongPress =
                    longPress?.let {
                        {
                            offset = 0f
                            it()
                        }
                    },
            ) {
                if (offset < 0f) offset = 0f else open()
            }
        }
    }
}

fun settledCategoryReveal(offset: Float, width: Float, threshold: Float): Float =
    if (offset <= -threshold) -width else 0f

/** Edición de la categoría y nota de un movimiento. */
@Composable
fun EditarEgresoPanel(
    expense: Egreso,
    categories: List<Categoria>,
    close: () -> Unit,
    pickingCategory: Boolean,
    selectCategory: (Boolean) -> Unit,
    hasReminder: Boolean,
    save: (Long?, String) -> Unit,
    reminder: () -> Unit,
) {
    var categoryId by rememberSaveable(expense.id) { mutableStateOf(expense.categoryId) }
    var note by rememberSaveable(expense.id) { mutableStateOf(expense.note.orEmpty()) }
    if (pickingCategory) {
        ContenidoPanel {
            TituloPanel("Cambiar categoría", { selectCategory(false) })
            (categories + listOf<Categoria?>(null)).forEach { option ->
                CategoriaOpcion(option, categoryId == option?.id, "expense-category-choice") {
                    categoryId = option?.id
                    selectCategory(false)
                }
            }
        }
        return
    }
    ContenidoPanel(acciones = {
        BotonPrincipal("Guardar cambios") { save(categoryId, note) }
    }) {
        TituloPanel("Detalle del egreso", close)
        Text(expense.description, style = MaterialTheme.typography.titleMedium)
        Text("−${money(expense.amount)}", color = Red, fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 4.dp))
        Text("${dateLabel(expense.day)} · ${expense.operation}", color = Muted, fontSize = 14.sp)
        HorizontalDivider(Modifier.padding(vertical = 15.dp), color = Line)
        Text("Categoría", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            val category = categories.find { it.id == categoryId }
            Row(
                Modifier.fillMaxWidth()
                    .testTag("expense-category")
                    .border(1.dp, Line, RoundedCornerShape(9.dp))
                    .clickable { selectCategory(true) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconoCategoria(category, 29)
                Text(
                    category?.name ?: "Sin categoría",
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f).padding(start = 9.dp),
                )
                Icon(Icons.Outlined.ExpandMore, null, Modifier.size(20.dp), tint = Blue)
            }
        }
        OutlinedTextField(
            note,
            { note = noteLimited(it) },
            modifier = Modifier.fillMaxWidth().testTag("expense-note"),
            singleLine = true,
            label = { Text("Nota (opcional)") },
            placeholder = { Text("Por ejemplo: Comida gato") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            supportingText = { Text("${noteLength(note)}/40", modifier = Modifier.fillMaxWidth()) },
            shape = RoundedCornerShape(9.dp),
        )
        if (expense.categoryId == SUBSCRIPTIONS_ID)
            TextButton(reminder, Modifier.fillMaxWidth()) {
                Text(if (hasReminder) "Ver recordatorio" else "Crear recordatorio desde este cobro")
            }
    }
}

@Composable
private fun CategoriaOpcion(category: Categoria?, checked: Boolean, tag: String, select: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .testTag("$tag-${category?.id ?: "none"}").clickable(onClick = select),
        shape = RoundedCornerShape(12.dp),
        color = if (checked) Color(0xFFEEF1FF) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (checked) Blue else Line),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconoCategoria(category, 32)
            Text(category?.name ?: "Sin categoría", Modifier.weight(1f).padding(start = 10.dp))
            if (checked) Icon(Icons.Outlined.Check, "Seleccionada", tint = Blue)
        }
    }
}

@Composable
fun SelectorCategoriaPanel(
    categories: List<Categoria>, count: Int, close: () -> Unit, apply: (Long?) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var touched by rememberSaveable { mutableStateOf(false) }
    ContenidoPanel(acciones = {
        BotonPrincipal("Aplicar", enabled = touched) { apply(selected) }
    }) {
        TituloPanel("Asignar categoría", close, "$count egresos seleccionados")
        (categories + listOf<Categoria?>(null)).forEach { category ->
            CategoriaOpcion(category, touched && selected == category?.id, "bulk") {
                selected = category?.id
                touched = true
            }
        }
    }
}
