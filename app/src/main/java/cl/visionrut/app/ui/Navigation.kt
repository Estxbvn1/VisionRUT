package cl.visionrut.app.ui

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.visionrut.app.ui.components.VisionRUTIcons
import cl.visionrut.app.ui.theme.Blue
import cl.visionrut.app.ui.theme.Ink
import cl.visionrut.app.ui.theme.Line
import cl.visionrut.app.ui.theme.Orange
import cl.visionrut.app.ui.theme.Pending
import cl.visionrut.app.util.money

/** Reglas del botón y gesto Atrás del sistema. */
enum class SystemBackAction {
    CLOSE_PANEL,
    SUMMARY,
    BANK_HOME,
    SYSTEM,
}

fun systemBackAction(inModule: Boolean, tab: Int, panelOpen: Boolean): SystemBackAction =
    when {
        panelOpen -> SystemBackAction.CLOSE_PANEL
        !inModule -> SystemBackAction.SYSTEM
        tab != 0 -> SystemBackAction.SUMMARY
        else -> SystemBackAction.BANK_HOME
    }

// Encabezado y salida del módulo.
@Composable
fun Encabezado(back: () -> Unit, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth()
        .background(Orange, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
        .statusBarsPadding()) {
        Row(
            Modifier.height(48.dp).fillMaxWidth().padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(back) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(23.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (action != null)
                TextButton(onAction) { Text(action, color = Color.White, fontSize = 15.sp) }
        }
    }
}

// Secciones principales y acciones de selección múltiple.
@Composable
fun BarraNavegacion(
    tab: Int,
    selecting: Boolean,
    selectedCount: Int,
    selectedAmount: Long,
    onCategorize: () -> Unit,
    onTabSelected: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
        if (selecting) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (selectedCount == 1) "1 seleccionado" else "$selectedCount seleccionados",
                        color = Blue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(money(selectedAmount), color = Blue, fontSize = 15.sp)
                }
                Button(
                    onCategorize,
                    enabled = selectedCount > 0,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(25.dp),
                ) {
                    Text("Categorizar", fontSize = 16.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf(
                    Icons.Outlined.BarChart to "Resumen",
                    Icons.AutoMirrored.Outlined.FormatListBulleted to "Egresos",
                    VisionRUTIcons.Bell to "Recordatorios",
                )
                .forEachIndexed { index, (icon, label) ->
                    Column(
                        Modifier.weight(1f).testTag("nav-$index").selectable(selected = tab == index, role = Role.Tab) {
                            onTabSelected(index)
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.height(2.dp)
                                .fillMaxWidth()
                                .background(if (tab == index) Orange else Line)
                        )
                        Icon(
                            icon,
                            label,
                            Modifier.padding(top = 10.dp).size(28.dp),
                            tint = if (tab == index) Orange else Ink,
                        )
                        Text(
                            label,
                            color = if (tab == index) Orange else Ink,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 11.dp),
                        )
                    }
                }
        }
    }
}

// Contenedor de paneles con estado y desplazamiento propios.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelInferior(
    sheet: String,
    stateKey: String,
    stateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val canDragSheet = sheet != "projection" && sheet != "categories"
    LaunchedEffect(sheet) { if (!state.isVisible) state.show() }
    val windowHeight =
        with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        sheetGesturesEnabled = canDragSheet,
        containerColor = Color.White,
        contentColor = Ink,
        dragHandle = if (canDragSheet) ({
            Box(
                Modifier.padding(top = 9.dp, bottom = 3.dp)
                    .size(38.dp, 4.dp)
                    .background(Pending, androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
            )
        }) else null,
    ) {
        stateHolder.SaveableStateProvider(stateKey) {
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = windowHeight * .84f)
                    .imePadding()
                    .padding(
                        start = 18.dp, end = 18.dp,
                        top = if (canDragSheet) 0.dp else 12.dp, bottom = 12.dp,
                    ),
            ) {
                if (canDragSheet) content()
                else CompositionLocalProvider(LocalOverscrollFactory provides null) { content() }
            }
        }
    }
}

/** Mantiene la acción principal visible mientras se desplaza el contenido del panel. */
@Composable
fun ContenidoPanel(
    acciones: @Composable () -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Column(
            Modifier.weight(1f, fill = false).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(bottom = 8.dp),
            content = contenido,
        )
        acciones()
    }
}
