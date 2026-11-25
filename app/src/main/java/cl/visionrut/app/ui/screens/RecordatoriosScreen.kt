package cl.visionrut.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.visionrut.app.data.Egreso
import cl.visionrut.app.data.IncludedCategories
import cl.visionrut.app.data.Recordatorio
import cl.visionrut.app.domain.CalculadorRecurrencia.billingDate
import cl.visionrut.app.domain.CalculadorResumen.expensesInMonth
import cl.visionrut.app.data.SUBSCRIPTIONS_ID
import cl.visionrut.app.ui.ContenidoPanel
import cl.visionrut.app.ui.components.SelectorMes
import cl.visionrut.app.util.money
import java.time.YearMonth
import cl.visionrut.app.domain.CalculadorRecurrencia.nextOccurrence
import cl.visionrut.app.domain.noteLimited
import cl.visionrut.app.ui.components.Aviso
import cl.visionrut.app.ui.components.BotonPrincipal
import cl.visionrut.app.ui.components.EgresoRow
import cl.visionrut.app.ui.components.IconoCategoria
import cl.visionrut.app.ui.components.Tarjeta
import cl.visionrut.app.ui.components.TituloPanel
import cl.visionrut.app.ui.theme.Blue
import cl.visionrut.app.ui.theme.Line
import cl.visionrut.app.ui.theme.Muted
import cl.visionrut.app.ui.theme.Pending
import cl.visionrut.app.ui.theme.Red
import cl.visionrut.app.util.dateLabel
import cl.visionrut.app.viewmodel.VisionRUTUiState
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Servicios recordados, estado de avisos y accesos de edición. */
@Composable
fun RecordatoriosScreen(
    data: VisionRUTUiState,
    permission: Boolean,
    scroll: LazyListState,
    settings: () -> Unit,
    requestPermission: () -> Unit,
    create: () -> Unit,
    edit: (Recordatorio) -> Unit,
    toggle: (Recordatorio, Boolean) -> Unit,
    preview: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            state = scroll,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                if (!permission)
                    Aviso(
                        "Notificaciones desactivadas\nActívalas para recibir avisos.",
                        true,
                        "Abrir ajustes",
                        settings,
                    )
                else
                    Aviso(
                        "Lleva el control de tus servicios\nCrea recordatorios a partir de tus cobros de Suscripciones."
                    )
            }
            if (!permission)
                item {
                    TextButton(requestPermission, modifier = Modifier.fillMaxWidth()) {
                        Text("Activar notificaciones", fontSize = 15.sp)
                    }
                }
            if (data.reminders.isEmpty())
                item {
                    Tarjeta {
                        Icon(
                            Icons.Outlined.NotificationsNone,
                            null,
                            Modifier.align(Alignment.CenterHorizontally).padding(12.dp).size(55.dp),
                            tint = Pending,
                        )
                        Text(
                            "Aún no tienes recordatorios",
                            Modifier.align(Alignment.CenterHorizontally),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Elige un cobro de Suscripciones para crear tu primer aviso.",
                            color = Muted,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            items(data.reminders, key = { it.id }) { reminder ->
                Tarjeta {
                    val expense = data.expenses.find { it.id == reminder.expenseId }
                    val category = data.categories.find { it.id == expense?.categoryId }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconoCategoria(category, 39)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(reminder.service, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                            Text(
                                if (reminder.frequency == "anual") "Anual" else "Mensual",
                                color = Muted,
                                fontSize = 14.sp,
                            )
                        }
                        TextButton(
                            { edit(reminder) },
                            enabled = expense != null,
                            contentPadding = PaddingValues(horizontal = 5.dp),
                        ) {
                            Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Editar", fontSize = 13.sp)
                        }
                        Switch(
                            reminder.enabled,
                            { toggle(reminder, it) },
                            modifier = Modifier.height(30.dp),
                            colors = SwitchDefaults.colors(checkedTrackColor = Blue),
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 11.dp), color = Line)
                    val due = reminder.billingDate()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(17.dp), tint = Muted)
                        Text(
                            "Próximo cobro",
                            fontSize = 14.sp,
                            color = Muted,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                        Text(dateLabel(due.toEpochDay()), fontSize = 14.sp, color = Muted)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Aviso",
                            fontSize = 14.sp,
                            color = Muted,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            when {
                                !permission || !reminder.enabled -> "Sin aviso"
                                reminder.lastNotifiedDue == due.toEpochDay() -> "Aviso enviado"
                                else -> dateLabel(due.minusDays(1).toEpochDay())
                            },
                            fontSize = 14.sp,
                            color = Muted,
                        )
                        TextButton(
                            onClick = { preview(reminder.id) },
                            enabled = reminder.enabled && permission,
                        ) {
                            Icon(Icons.Outlined.NotificationsActive, "Enviar aviso de ${reminder.service}", Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Enviar ahora", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BotonPrincipal(
                "Crear recordatorio",
                icon = Icons.Outlined.AddCircleOutline,
                onClick = create,
            )
            Text(
                "Selecciona un egreso de Suscripciones.",
                fontSize = 14.sp,
                color = Muted,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

@Composable
fun ElegirSuscripcionPanel(
    data: VisionRUTUiState, month: YearMonth, onMonth: (YearMonth) -> Unit,
    close: () -> Unit, choose: (Egreso) -> Unit,
) {
    ContenidoPanel {
        TituloPanel("Elegir suscripción", close)
        SelectorMes(month, data.months, onMonth)
        val charges = expensesInMonth(data.expenses, month).filter { it.categoryId == SUBSCRIPTIONS_ID }
        val reminders = data.reminders.associateBy { it.expenseId }
        if (charges.isEmpty()) Aviso("No hay cobros de suscripciones este mes. Puedes consultar otro mes.")
        charges.forEach { expense ->
            val reminder = reminders[expense.id]
            Row(
                Modifier.fillMaxWidth().testTag("charge-${expense.id}")
                    .clickable(enabled = reminder == null) { choose(expense) }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(expense.description, fontWeight = FontWeight.Medium)
                    Text("${money(expense.amount)} · ${dateLabel(expense.day)}", color = Muted)
                }
                if (reminder != null) Text("Ya creado", color = Muted)
                else Icon(Icons.Outlined.ChevronRight, "Elegir cobro", tint = Blue)
            }
            HorizontalDivider(color = Line)
        }
    }
}

/** Configuración o eliminación del aviso de una suscripción. */
@Composable
fun EditarRecordatorioPanel(
    expense: Egreso,
    original: Recordatorio?,
    permission: Boolean,
    close: () -> Unit,
    changeCharge: (() -> Unit)? = null,
    delete: () -> Unit = {},
    save: (String, LocalDate, String) -> Unit,
) {
    val context = LocalContext.current
    val initialDate =
        original?.billingDate()
            ?: LocalDate.ofEpochDay(expense.day).plusMonths(1).let {
                nextOccurrence(it, "mensual", LocalDate.now().plusDays(1))
            }
    var name by
        rememberSaveable(expense.id, original?.id) { mutableStateOf(original?.service ?: "") }
    var day by
        rememberSaveable(expense.id, original?.id) { mutableLongStateOf(initialDate.toEpochDay()) }
    var frequency by
        rememberSaveable(expense.id, original?.id) {
            mutableStateOf(original?.frequency ?: "mensual")
        }
    var confirmDelete by remember { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(day)
    val validDate = date > LocalDate.now() || (date == LocalDate.now() && original?.billingDate() == date)
    ContenidoPanel(acciones = {
        BotonPrincipal(if (original == null) "Guardar recordatorio" else "Guardar cambios",
            enabled = name.isNotBlank() && validDate) { save(name, date, frequency) }
    }) {
        TituloPanel(if (original == null) "Crear recordatorio" else "Editar recordatorio", close)
        if (changeCharge != null) TextButton(changeCharge) { Text("Cambiar suscripción") }
        EgresoRow(
            expense,
            IncludedCategories.first { it.id == SUBSCRIPTIONS_ID },
            interactive = false,
            onClick = {},
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            name,
            { name = noteLimited(it) },
            Modifier.fillMaxWidth().testTag("reminder-name"),
            label = { Text("Nombre del servicio") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(9.dp),
        )
        Spacer(Modifier.height(13.dp))
        Text("Próximo cobro", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        OutlinedButton(
            {
                DatePickerDialog(
                        context,
                        { _, year, month, dom ->
                            day = LocalDate.of(year, month + 1, dom).toEpochDay()
                        },
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth,
                    )
                    .apply {
                        datePicker.minDate =
                            LocalDate.now()
                                .plusDays(1)
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                    }
                    .show()
            },
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(9.dp),
            contentPadding = PaddingValues(12.dp),
        ) {
            Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(19.dp))
            Text(
                date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                Modifier.weight(1f).padding(start = 9.dp),
            )
            Icon(Icons.Outlined.ExpandMore, "Elegir fecha", Modifier.size(18.dp))
        }
        Text(
            "Frecuencia",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("mensual" to "Mensual", "anual" to "Anual").forEach { (value, label) ->
                FilterChip(
                    frequency == value,
                    { frequency = value },
                    label = { Text(label, Modifier.fillMaxWidth(), fontSize = 15.sp) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            "Aviso un día antes",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth()
                .background(Color(0xFFF3F5FD), RoundedCornerShape(8.dp))
                .padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.NotificationsNone, null, Modifier.size(19.dp), tint = Blue)
            Text(
                "${dateLabel(date.minusDays(1).toEpochDay())} · desde las 09:00",
                fontSize = 15.sp,
                color = Blue,
                modifier = Modifier.padding(start = 9.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        if (!permission)
            Aviso(
                "Al guardar, podrás autorizar las notificaciones. Si están bloqueadas, abre los ajustes para recibir avisos.",
                true,
            )
        else Aviso("El aviso no paga ni cancela tu suscripción.")
        if (!validDate) Text("El próximo cobro debe ser desde mañana.", fontSize = 15.sp, color = Red)
        Spacer(Modifier.height(14.dp))
        if (original != null) {
            TextButton({ confirmDelete = true }, Modifier.fillMaxWidth().testTag("delete-reminder")) {
                Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(19.dp), tint = Red)
                Text("Eliminar recordatorio", color = Red, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
    if (confirmDelete)
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar este recordatorio?") },
            text = {
                Text(
                    "Dejarás de recibir este aviso. El cobro y su nota se conservarán; tu suscripción seguirá activa con el proveedor."
                )
            },
            confirmButton = {
                TextButton(
                    {
                        confirmDelete = false
                        delete()
                    },
                    Modifier.testTag("confirm-delete-reminder"),
                ) {
                    Text("Eliminar", color = Red)
                }
            },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancelar") } },
        )
}
