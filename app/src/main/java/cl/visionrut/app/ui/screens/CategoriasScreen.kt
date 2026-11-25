package cl.visionrut.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.visionrut.app.data.Categoria
import cl.visionrut.app.data.IncludedCategories
import cl.visionrut.app.ui.ContenidoPanel
import cl.visionrut.app.ui.components.BotonPrincipal
import cl.visionrut.app.ui.components.IconoCategoria
import cl.visionrut.app.ui.components.TituloPanel
import cl.visionrut.app.ui.components.iconoCategoria
import cl.visionrut.app.ui.theme.Blue
import cl.visionrut.app.ui.theme.Line
import cl.visionrut.app.ui.theme.Muted
import cl.visionrut.app.viewmodel.VisionRUTUiState

/** Listado de categorías incluidas y propias. */
@Composable
fun CategoriasPanel(
    data: VisionRUTUiState,
    close: () -> Unit,
    create: () -> Unit,
    delete: (Categoria) -> Unit,
    view: (Categoria) -> Unit,
) {
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    val target = data.categories.find { it.id == pendingDelete && !it.included }
    if (target != null) AlertDialog(
        onDismissRequest = { pendingDelete = null },
        title = { Text("¿Eliminar ${target.name}?") },
        text = { Text("Sus egresos pasarán a Sin categoría y conservarán sus notas.") },
        confirmButton = {
            TextButton({
                pendingDelete = null
                delete(target)
            }) { Text("Eliminar") }
        },
        dismissButton = { TextButton({ pendingDelete = null }) { Text("Cancelar") } },
    )
    TituloPanel("Gestionar categorías", close)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Categorías incluidas",
            fontSize = 15.sp,
            color = Muted,
            modifier = Modifier.weight(1f),
        )
        TextButton(create, contentPadding = PaddingValues(4.dp)) {
            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
            Text("Crear", fontSize = 16.sp)
        }
    }
    data.categories
        .filter { it.included }
        .forEach { category ->
            FilaCategoria(category, false, { view(category) }, {})
        }
    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Line)
    Text("Tus categorías", fontSize = 15.sp, color = Muted)
    val custom = data.categories.filter { !it.included }
    if (custom.isEmpty())
        Text(
            "Crea una categoría para organizar a tu manera.",
            color = Muted,
            fontSize = 15.sp,
            modifier = Modifier.padding(vertical = 15.dp),
        )
    custom.forEach { category ->
        FilaCategoria(category, true, { view(category) }, { pendingDelete = category.id })
    }
}

@Composable
private fun FilaCategoria(category: Categoria, personal: Boolean, view: () -> Unit, delete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = view),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoCategoria(category, 34)
        Text(category.name, modifier = Modifier.weight(1f).padding(start = 10.dp), fontSize = 16.sp)
        if (personal)
            IconButton(delete, Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    "Eliminar ${category.name}",
                    Modifier.size(19.dp),
                    tint = Blue,
                )
            }
        else
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                null,
                Modifier.size(19.dp),
                tint = Blue,
            )
    }
}

/** Nueva categoría con icono y color. */
@Composable
fun CrearCategoriaPanel(
    categories: List<Categoria>,
    close: () -> Unit,
    save: (String, String, Long) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf("health") }
    var color by
        rememberSaveable { mutableLongStateOf(0xFF25B84B) }
    val duplicate = categories.any { it.name.equals(name.trim(), true) }
    ContenidoPanel(acciones = {
        BotonPrincipal("Crear categoría", enabled = name.isNotBlank() && !duplicate) {
            save(name, icon, color)
        }
    }) {
        TituloPanel("Crear categoría", close)
        OutlinedTextField(
            name,
            { name = it.take(30) },
            Modifier.fillMaxWidth().testTag("category-name"),
            label = { Text("Nombre de la categoría") },
            singleLine = true,
            isError = duplicate,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            supportingText = {
                Text(if (duplicate) "Ya existe una categoría con este nombre" else "${name.length}/30")
            },
            shape = RoundedCornerShape(9.dp),
        )
        Spacer(Modifier.height(9.dp))
        Text("Icono", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        val icons = IconosNuevosCategoria
        icons.chunked(6).forEach { group ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                group.forEach { option ->
                    Box(
                        Modifier.padding(vertical = 5.dp)
                            .size(42.dp)
                            .background(
                                if (option == icon) Color(0xFFEDF1FF) else Color.Transparent,
                                CircleShape,
                            )
                            .then(
                                if (option == icon) Modifier.border(1.5.dp, Blue, CircleShape)
                                else Modifier
                            )
                            .clickable { icon = option },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            iconoCategoria(option),
                            nombreIconoCategoria(option),
                            Modifier.size(23.dp),
                            tint = Blue,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Color", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        ColoresCategoria.chunked(8).forEach { group ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                group.forEach { option ->
                    Box(
                        Modifier.size(36.dp)
                            .clickable(onClickLabel = "Elegir color") { color = option }
                            .then(
                                if (option == color) Modifier.border(2.dp, Blue, CircleShape)
                                else Modifier
                            )
                            .padding(4.dp)
                            .background(Color(option), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (option == color)
                            Icon(
                                Icons.Outlined.Check,
                                "Color seleccionado",
                                Modifier.size(18.dp),
                                tint = Color.White,
                            )
                    }
                }
            }
        }
        Text("Vista previa", fontSize = 14.sp, color = Muted)
        Row(
            Modifier.fillMaxWidth()
                .padding(vertical = 9.dp)
                .background(Color(color).copy(alpha = .08f), RoundedCornerShape(9.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconoCategoria(Categoria(name = name, icon = icon, color = color), 30)
            Text(name.ifBlank { "Tu categoría" }, Modifier.padding(start = 10.dp), fontSize = 16.sp)
        }
    }
}

// Iconos y colores disponibles para personalizar categorías.
private val IconosNuevosCategoria =
    listOf(
        "toll",
        "transfers",
        "home",
        "car",
        "health",
        "study",
        "work",
        "travel",
        "gift",
        "sport",
        "tools",
        "beauty",
        "shopping",
        "perfume",
        "sneakers",
        "clothes",
        "motorcycle",
        "delivery",
    )

private val ColoresCategoria =
    listOf(
        0xFFFF7900L,
        0xFF1685FFL,
        0xFF8850F6L,
        0xFF00B8C4L,
        0xFFFF2BA8L,
        0xFFFFB600L,
        0xFF25B84BL,
        0xFF98A2B7L,
        0xFFB4441EL,
        0xFF3655A7L,
        0xFF5C3096L,
        0xFF007A82L,
        0xFFC82D78L,
        0xFF9B6D00L,
        0xFF167A42L,
        0xFF465167L,
    )

private fun nombreIconoCategoria(key: String): String =
    when (key) {
        "toll" -> "Autopista y TAG"
        "transfers" -> "Transferencias"
        "home" -> "Hogar"
        "car" -> "Auto"
        "health" -> "Salud"
        "study" -> "Estudios"
        "work" -> "Trabajo"
        "travel" -> "Viajes"
        "gift" -> "Regalos"
        "sport" -> "Deporte"
        "tools" -> "Mantención"
        "perfume" -> "Perfume"
        "sneakers" -> "Zapatillas"
        "clothes" -> "Ropa"
        "motorcycle" -> "Moto"
        "delivery" -> "Delivery"
        "beauty" -> "Cuidado personal"
        else -> IncludedCategories.find { it.icon == key }?.name ?: "Favorito"
    }
