package cl.visionrut.app.domain

internal fun normalizarNombreCategoria(name: String): String {
    val trimmed = name.trim()
    require(trimmed.isNotEmpty() && trimmed.length <= 30)
    return trimmed
}

internal fun nombreCategoriaDuplicado(name: String, existingNames: List<String>): Boolean {
    val trimmed = name.trim()
    // La opción integrada representa categoria_id = NULL y no aparece entre las filas de Room.
    return trimmed.equals("Sin categoría", ignoreCase = true) ||
        existingNames.any { it.equals(trimmed, ignoreCase = true) }
}
