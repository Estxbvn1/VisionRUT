package cl.visionrut.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.visionrut.app.data.Categoria
import cl.visionrut.app.data.Egreso
import cl.visionrut.app.ui.theme.Alert
import cl.visionrut.app.ui.theme.Background
import cl.visionrut.app.ui.theme.Blue
import cl.visionrut.app.ui.theme.Ink
import cl.visionrut.app.ui.theme.Muted
import cl.visionrut.app.ui.theme.Orange
import cl.visionrut.app.ui.theme.Pending
import cl.visionrut.app.ui.theme.Red
import cl.visionrut.app.util.dateLabel
import cl.visionrut.app.util.money
import cl.visionrut.app.util.monthLabel
import java.time.YearMonth

@Composable
fun Tarjeta(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun BotonPrincipal(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick,
        modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TituloPanel(title: String, close: () -> Unit, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (subtitle != null)
                Text(subtitle, color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
        }
        IconButton(close, Modifier.size(44.dp)) {
            Icon(Icons.Outlined.Close, "Cerrar panel", Modifier.size(21.dp), tint = Muted)
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
fun Aviso(
    text: String,
    warning: Boolean = false,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth()
            .background(
                if (warning) Color(0xFFFFF1CA) else Color(0xFFEDF1FF),
                RoundedCornerShape(9.dp),
            )
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (warning) Icons.Outlined.WarningAmber else Icons.Outlined.Info,
            null,
            Modifier.size(20.dp),
            tint = if (warning) Color(0xFF9B6800) else Blue,
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = if (warning) Color(0xFF785400) else Blue,
            modifier = Modifier.weight(1f),
        )
        if (action != null)
            TextButton(onAction, contentPadding = PaddingValues(4.dp)) {
                Text(action, fontSize = 14.sp)
            }
    }
}

/** Permite reintentar la carga cuando no se pudo obtener el estado inicial. */
@Composable
fun ErrorCarga(message: String, retry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Background).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        BotonPrincipal("Reintentar", onClick = retry)
    }
}

@Composable
fun FilaAccion(title: String, icon: ImageVector, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .testTag("action-$title")
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = Blue)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Blue, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            if (subtitle != null)
                Text(subtitle, color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            null,
            Modifier.size(20.dp),
            tint = Blue,
        )
    }
}

@Composable
fun SelectorMes(month: YearMonth, months: Set<String>, onChange: (YearMonth) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = YearMonth.now()
    val available =
        months
            .mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }
            .filter { it <= current }
            .sortedDescending()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(
                { onChange(month.minusMonths(1)) },
                enabled = month.minusMonths(1) in available,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Outlined.ChevronLeft,
                    "Mes anterior",
                    Modifier.size(20.dp),
                    tint = if (month.minusMonths(1) in available) Blue else Pending,
                )
            }
            Box {
                Row(
                    Modifier.clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Orange.copy(alpha = .6f), RoundedCornerShape(24.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(16.dp), tint = Blue)
                    Spacer(Modifier.width(6.dp))
                    Text(monthLabel(month), fontSize = 15.sp, color = Ink)
                }
                DropdownMenu(expanded, { expanded = false }) {
                    available.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(monthLabel(m)) },
                            onClick = {
                                onChange(m)
                                expanded = false
                            },
                        )
                    }
                }
            }
            IconButton(
                { onChange(month.plusMonths(1)) },
                enabled = month < current,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    "Mes siguiente",
                    Modifier.size(20.dp),
                    tint = if (month < current) Blue else Pending,
                )
            }
        }
        if (month == current) {
            Text(
                "Mes actual",
                color = Color(0xFF946000),
                fontSize = 14.sp,
                modifier =
                    Modifier.background(Color(0xFFFFEDC4), RoundedCornerShape(12.dp))
                        .padding(horizontal = 9.dp, vertical = 2.dp),
            )
        } else {
            Text(
                "Volver al mes actual",
                color = Blue,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onChange(current) }.padding(3.dp),
            )
        }
    }
}

@Composable
fun IconoCategoria(category: Categoria?, size: Int = 36) {
    Box(
        Modifier.size(size.dp)
            .background(category?.let { Color(it.color) } ?: Alert.copy(alpha = .20f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            category?.let { iconoCategoria(it.icon) } ?: Icons.Outlined.WarningAmber,
            null,
            Modifier.size((size * .6f).dp),
            tint = if (category == null) Color(0xFF9B6800) else Color.White,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EgresoRow(
    expense: Egreso,
    category: Categoria?,
    selecting: Boolean = false,
    selected: Boolean = false,
    interactive: Boolean = true,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .testTag("expense-${expense.id}")
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .then(
                if (interactive)
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongPress,
                        onLongClickLabel = "Seleccionar egreso",
                    )
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) {
            Checkbox(
                selected,
                null,
                modifier = Modifier.padding(end = 8.dp).size(22.dp),
                colors = CheckboxDefaults.colors(checkedColor = Blue),
            )
        }
        IconoCategoria(category, 36)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    expense.description,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = if (interactive) 2 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "−${money(expense.amount)}",
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    color = Red,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    dateLabel(expense.day),
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    color = Muted,
                    modifier = Modifier.weight(1f),
                )
                if (category == null)
                    Icon(
                        Icons.Outlined.WarningAmber,
                        null,
                        Modifier.size(15.dp),
                        tint = Color(0xFF9B6800),
                    )
                else Box(Modifier.size(7.dp).background(Color(category.color), CircleShape))
                Text(
                    category?.name ?: "Sin categoría",
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    color = Muted,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp).widthIn(max = 114.dp),
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!expense.note.isNullOrEmpty())
                Text(
                    expense.note,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    color = Blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp),
                )
        }
        if (interactive && !selecting)
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                null,
                Modifier.padding(start = 4.dp).size(20.dp),
                tint = Blue,
            )
    }
}
