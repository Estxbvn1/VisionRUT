package cl.visionrut.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.visionrut.app.data.SUBSCRIPTIONS_ID
import cl.visionrut.app.notifications.ReminderScheduler
import cl.visionrut.app.ui.components.ErrorCarga
import cl.visionrut.app.ui.screens.CategoriasPanel
import cl.visionrut.app.ui.screens.CrearCategoriaPanel
import cl.visionrut.app.ui.screens.ElegirSuscripcionPanel
import cl.visionrut.app.ui.screens.EditarEgresoPanel
import cl.visionrut.app.ui.screens.EditarRecordatorioPanel
import cl.visionrut.app.ui.screens.EgresosScreen
import cl.visionrut.app.ui.screens.InicioScreen
import cl.visionrut.app.ui.screens.ProyeccionPanel
import cl.visionrut.app.ui.screens.RecordatoriosScreen
import cl.visionrut.app.ui.screens.ResumenScreen
import cl.visionrut.app.ui.screens.SelectorCategoriaPanel
import cl.visionrut.app.ui.theme.Background
import cl.visionrut.app.ui.theme.Blue
import cl.visionrut.app.viewmodel.VisionRUTViewModel
import cl.visionrut.app.viewmodel.VisionRUTViewModelFactory
import java.time.YearMonth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Coordina las secciones, los permisos y los paneles del módulo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionRUTApp(
    reminderRequest: Int = 0,
    vm: VisionRUTViewModel = viewModel(factory = VisionRUTViewModelFactory),
) {
    // Estado del ViewModel y estado visual de navegación.
    val data by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val accessibility = LocalAccessibilityManager.current
    var confirmation by remember { mutableStateOf("") }
    var confirmationVisible by remember { mutableStateOf(false) }
    var confirmationVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(confirmationVersion) {
        if (confirmationVersion > 0) {
            delay(accessibility?.calculateRecommendedTimeoutMillis(1500L, containsText = true) ?: 1500L)
            confirmationVisible = false
        }
    }
    var home by rememberSaveable { mutableStateOf(true) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val month = data.selectedMonth
    val filter = data.categoryFilter
    val selecting = data.isSelecting
    val selectedIds = data.selectedExpenseIds
    var chargeMonth by rememberSaveable { mutableStateOf(month.toString()) }
    var pendingIds by rememberSaveable { mutableStateOf(longArrayOf()) }
    var pendingCategory by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingNote by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingAlarms by rememberSaveable { mutableStateOf(longArrayOf()) }
    var panelGeneration by rememberSaveable { mutableIntStateOf(0) }
    val panelStateHolder = rememberSaveableStateHolder()
    var sheet by rememberSaveable { mutableStateOf("") }
    var expenseId by rememberSaveable { mutableLongStateOf(-1) }
    var reminderId by rememberSaveable { mutableLongStateOf(-1) }
    var bankInfo by remember { mutableStateOf(false) }
    var permitted by remember { mutableStateOf(ReminderScheduler.permitted(context)) }
    var saving by remember { mutableStateOf(false) }
    val summaryScroll = rememberLazyListState()
    val expenseScroll = rememberLazyListState()
    val reminderScroll = rememberLazyListState()
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            permitted = ReminderScheduler.permitted(context)
            vm.restoreReminders()
        }
    fun settings() {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
    }
    fun askPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !permitted)
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else if (!permitted) settings()
    }
    // Navegación entre secciones y apertura de paneles.
    fun visit(target: Int, targetFilter: String = filter) {
        if (!home && tab == target && filter == targetFilter) return
        home = false
        tab = target
        vm.selectFilter(targetFilter)
        vm.cancelSelection()
    }

    fun panelKey(target: String = sheet): String =
        when (target) {
            "expense", "expenseCategory" -> "expense:$expenseId"
            "reminder" -> "reminder:$expenseId:$reminderId"
            "bulk" -> "bulk:${selectedIds.joinToString(",")}"
            else -> target
        }
    fun showPanel(target: String) {
        panelGeneration++
        if (sheet.isNotEmpty()) panelStateHolder.removeState(panelKey())
        panelStateHolder.removeState(panelKey(target))
        sheet = target
    }
    fun closePanel() {
        if (saving) return
        if (sheet == "expenseCategory") {
            panelGeneration++
            sheet = "expense"
            return
        }
        panelGeneration++
        panelStateHolder.removeState(panelKey())
        sheet = ""
    }
    fun closeAllPanels() {
        panelGeneration++
        panelStateHolder.removeState(panelKey())
        sheet = ""
    }
    fun exitModule() {
        closeAllPanels()
        home = true
        vm.cancelSelection()
    }

    fun back() {
        when (systemBackAction(!home, tab, sheet.isNotEmpty())) {
            SystemBackAction.CLOSE_PANEL -> closePanel()
            SystemBackAction.SUMMARY -> {
                tab = 0
                vm.cancelSelection()
            }
            SystemBackAction.BANK_HOME -> exitModule()
            SystemBackAction.SYSTEM -> Unit
        }
    }

    fun changeMonth(value: YearMonth) {
        vm.selectMonth(value)
        expenseScroll.requestScrollToItem(0)
        summaryScroll.requestScrollToItem(0)
    }

    fun openExpenses(value: String) {
        closeAllPanels()
        visit(1, value)
        expenseScroll.requestScrollToItem(0)
    }
    fun saved(message: String) {
        saving = false
        closeAllPanels()
        confirmation = message
        confirmationVisible = true
        confirmationVersion++
    }
    fun commitChange(ids: Set<Long>, category: Long?, note: String?, alarms: Set<Long>) {
        if (saving || ids.isEmpty()) return
        val swipe = sheet.isEmpty()
        val previous = if (swipe) data.expenses.find { it.id in ids }?.categoryId else null
        val bulk = sheet == "bulk"
        saving = true
        vm.changeExpenses(ids, category, note, alarms) {
            if (swipe && previous != null && alarms.isEmpty()) {
                saving = false
                scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    if (snackbar.showSnackbar("Egreso sin categoría", actionLabel = "Deshacer",
                            duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed)
                        vm.changeExpenses(ids, previous) {}
                }
            } else saved(when {
                bulk && category == null -> if (ids.size == 1) "1 egreso sin categoría" else "${ids.size} egresos sin categoría"
                bulk -> if (ids.size == 1) "1 egreso categorizado" else "${ids.size} egresos categorizados"
                alarms.isNotEmpty() -> "Egreso actualizado y aviso cancelado"
                else -> "Egreso actualizado"
            })
        }
    }

    fun requestChange(ids: Set<Long>, category: Long?, note: String? = null) {
        if (saving) return
        val alarms = if (category == SUBSCRIPTIONS_ID) emptyList() else
            data.suscripciones.filter { it.pierdeTodosLosCobros(ids) }.mapNotNull { it.aviso }
        if (alarms.isEmpty()) commitChange(ids, category, note, emptySet())
        else {
            pendingIds = ids.toLongArray()
            pendingCategory = category
            pendingNote = note
            pendingAlarms = alarms.map { it.id }.toLongArray()
        }
    }
    // Eventos de la interfaz, permisos y accesos desde notificaciones.
    LaunchedEffect(reminderRequest) {
        if (reminderRequest > 0) {
            if (home) visit(0)
            visit(2)
        }
    }
    LaunchedEffect(Unit) {
        vm.errors.collectLatest { message ->
            saving = false
            confirmationVisible = false
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message, actionLabel = "Cerrar", duration = SnackbarDuration.Indefinite)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permitted = ReminderScheduler.permitted(context)
        vm.restoreReminders()
    }
    SideEffect {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle =
                if (home) SystemBarStyle.light(0xFFAA5D15.toInt(), 0xFFAA5D15.toInt())
                else SystemBarStyle.dark(0xFFFF7900.toInt()),
            navigationBarStyle = SystemBarStyle.light(-1, 0xFF23262D.toInt()),
        )
    }
    BackHandler(!home && sheet.isEmpty(), onBack = ::back)

    // Las pantallas consumen el estado y envían sus acciones al ViewModel.
    if (data.loading) {
        Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Blue)
        }
    } else if (data.errorMessage != null && data.expenses.isEmpty()) {
        ErrorCarga(checkNotNull(data.errorMessage)) { vm.reload() }
    } else if (home) {
        InicioScreen(
            open = { visit(0) },
            movements = { visit(1, "all") },
            info = { bankInfo = true },
        )
    } else {
        Scaffold(
            containerColor = Background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                Encabezado(
                    ::exitModule,
                    when {
                        selecting -> "Cancelar"
                        tab == 1 -> "Seleccionar"
                        else -> null
                    },
                ) {
                    vm.toggleSelectionMode()
                }
            },
            bottomBar = {
                BarraNavegacion(
                    tab,
                    selecting,
                    selectedIds.size,
                    data.expenses.filter { it.id in selectedIds }.sumOf { it.amount },
                    onCategorize = { showPanel("bulk") },
                    onTabSelected = { index ->
                        visit(index)
                    },
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().widthIn(max = 680.dp)) {
                when (tab) {
                    0 ->
                        ResumenScreen(
                            data,
                            month,
                            summaryScroll,
                            ::changeMonth,
                            ::openExpenses,
                            { showPanel("categories") },
                            { showPanel("projection") },
                        )
                    1 ->
                        EgresosScreen(
                            data,
                            month,
                            filter,
                            expenseScroll,
                            selecting,
                            selectedIds.toSet(),
                            ::changeMonth,
                            onFilter = {
                                vm.selectFilter(it)
                                expenseScroll.requestScrollToItem(0)
                            },
                            clearFilter = { openExpenses("all") },
                            onLongPress = { expense ->
                                vm.startSelection(expense.id)
                            },
                            onClearCategory = { requestChange(setOf(it.id), null) },
                        ) { expense ->
                            when {
                                selecting -> vm.toggleSelectedExpense(expense.id)
                                else -> {
                                    expenseId = expense.id
                                    showPanel("expense")
                                }
                            }
                        }
                    else ->
                        RecordatoriosScreen(
                            data,
                            permitted,
                            reminderScroll,
                            ::settings,
                            ::askPermission,
                            create = {
                                chargeMonth = month.toString()
                                showPanel("chooseCharge")
                            },
                            edit = { reminder ->
                                expenseId = reminder.expenseId
                                reminderId = reminder.id
                                showPanel("reminder")
                            },
                            toggle = { reminder, enabled ->
                                vm.toggle(reminder, enabled)
                                if (enabled && !permitted) askPermission()
                            },
                            preview = { id ->
                                vm.previewReminder(id) { service ->
                                    scope.launch {
                                        snackbar.currentSnackbarData?.dismiss()
                                        snackbar.showSnackbar("Aviso de $service enviado")
                                    }
                                }
                            },
                        )
                }
                AnimatedVisibility(
                    visible = confirmationVisible,
                    modifier = Modifier.align(Alignment.Center),
                    enter = fadeIn(), exit = fadeOut(),
                ) { ConfirmacionGuardada(confirmation) }
            }
        }
    }
    if (bankInfo)
        AlertDialog(
            onDismissRequest = { bankInfo = false },
            title = { Text("Inicio de demostración") },
            text = {
                Text(
                    "Esta pantalla representa el acceso bancario con datos ficticios. Entra a VisionRUT para organizar tus egresos."
                )
            },
            confirmButton = { TextButton({ bankInfo = false }) { Text("Entendido") } },
        )

    // Un solo contenedor inferior representa el panel seleccionado.
    if (sheet.isNotEmpty()) {
        val visibleGeneration = panelGeneration
        PanelInferior(
            sheet,
            panelKey(),
            panelStateHolder,
            onDismiss = { if (panelGeneration == visibleGeneration) closePanel() },
        ) {
            when (sheet) {
                "categories" -> ContenidoPanel {
                    CategoriasPanel(data, ::closePanel, { showPanel("custom") },
                        delete = { category ->
                            if (!saving) {
                                saving = true
                                vm.deleteCategory(category) { saved("Categoría eliminada") }
                            }
                        },
                        view = { openExpenses(it.id.toString()) },
                    )
                }
                "custom" -> CrearCategoriaPanel(data.categories, ::closePanel) { name, icon, color ->
                    if (!saving) {
                        saving = true
                        vm.createCategory(name, icon, color) { saved("Categoría creada") }
                    }
                }
                "expense", "expenseCategory" -> data.expenses.find { it.id == expenseId }?.let { expense ->
                    val reminder = data.suscripciones.find { it.contiene(expense.id) }?.aviso
                    EditarEgresoPanel(expense, data.categories, ::closePanel,
                        pickingCategory = sheet == "expenseCategory",
                        selectCategory = { choosing ->
                            panelGeneration++
                            sheet = if (choosing) "expenseCategory" else "expense"
                        },
                        reminderEnabled = reminder?.enabled,
                        save = { category, note -> requestChange(setOf(expense.id), category, note) },
                        reminder = {
                            reminderId = reminder?.id ?: -1
                            showPanel("reminder")
                        },
                    )
                }
                "bulk" -> SelectorCategoriaPanel(data.categories, selectedIds.size, ::closePanel) {
                    requestChange(selectedIds, it)
                }
                "projection" -> ContenidoPanel { ProyeccionPanel(data, ::closePanel) }
                "chooseCharge" -> ElegirSuscripcionPanel(data, YearMonth.parse(chargeMonth),
                    { chargeMonth = it.toString() }, ::closePanel) { expense ->
                    expenseId = expense.id
                    reminderId = -1
                    showPanel("reminder")
                }
                "reminder" ->
                    data.expenses
                        .find { it.id == expenseId }
                        ?.let { expense ->
                            val original = data.reminders.find { it.id == reminderId }
                            EditarRecordatorioPanel(
                                expense,
                                original,
                                permitted,
                                ::closePanel,
                                changeCharge = if (original == null && tab == 2)
                                    ({ showPanel("chooseCharge") }) else null,
                                delete = {
                                    if (original != null && !saving) {
                                        saving = true
                                        vm.deleteReminder(original) {
                                            saved("Recordatorio eliminado")
                                        }
                                    }
                                },
                            ) { name, date, frequency ->
                                if (!saving) {
                                    saving = true
                                    vm.saveReminder(original, expense, name, date, frequency) {
                                        saved("Recordatorio guardado")
                                        if (!permitted) askPermission()
                                    }
                                }
                            }
                        }
            }
            data.errorMessage?.let { message ->
                if (data.expenses.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error)
            }
            if (saving)
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp), color = Blue)
        }
    }
    if (pendingAlarms.isNotEmpty()) {
        val count = pendingAlarms.size
        AlertDialog(
            onDismissRequest = { pendingAlarms = longArrayOf() },
            title = { Text(if (count == 1) "¿Eliminar el recordatorio asociado?" else "¿Eliminar $count recordatorios?") },
            text = { Text(if (count == 1) "Este cambio deja a la suscripción sin cobros asociados. También se eliminará su recordatorio."
                else "Este cambio deja a $count suscripciones sin cobros asociados. También se eliminarán sus recordatorios.") },
            confirmButton = { TextButton({
                val confirmed = pendingAlarms.toSet()
                pendingAlarms = longArrayOf()
                commitChange(pendingIds.toSet(), pendingCategory, pendingNote, confirmed)
            }) { Text("Reclasificar y cancelar avisos") } },
            dismissButton = { TextButton({ pendingAlarms = longArrayOf() }) { Text("Cancelar") } },
        )
    }

}

@Composable
private fun ConfirmacionGuardada(message: String) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 280.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(42.dp).background(Color(0xFFE3F4E9), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, tint = Color(0xFF247446))
            }
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        }
    }
}
