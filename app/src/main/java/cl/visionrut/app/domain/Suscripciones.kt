package cl.visionrut.app.domain

import cl.visionrut.app.data.Egreso
import cl.visionrut.app.data.Recordatorio
import cl.visionrut.app.data.SUBSCRIPTIONS_ID
import java.util.Locale

private val espacios = Regex("\\s+")
private val spotify = Regex("""spotify(?:[ *]+(?:p[a-z0-9]+|premium))?""")
private val netflix = Regex("""netflix(?:\.com)?(?: [\d() +\-]+)?""")
private val glosasGenericas = setOf("compra", "compra tarjeta", "compra con tarjeta", "pos compra debito",
    "pos compra débito", "compra debito", "compra con tarjeta de débito", "compra tarjeta digital",
    "pago en linea", "pago en línea", "cobro recurrente", "mercadopago", "mercado pago", "merpago",
    "transbank", "servipag", "servipag.com", "servipag (www.servipag.com)", "")

internal fun Egreso.claveSuscripcion(): String {
    val glosa = description.trim().lowercase(Locale.ROOT).replace(espacios, " ")
    return when {
        spotify.matches(glosa) -> "spotify"
        netflix.matches(glosa) -> "netflix"
        glosa in glosasGenericas -> "egreso:$id"
        else -> "comercio:$glosa"
    }
}

data class Suscripcion(val cobros: List<Egreso>, val avisos: List<Recordatorio>) {
    // En instalaciones antiguas pueden existir varios avisos: prevalece el último creado.
    val aviso = avisos.maxByOrNull { it.id }
    fun contiene(egresoId: Long) = cobros.any { it.id == egresoId }
    fun pierdeTodosLosCobros(ids: Set<Long>) = cobros.all { it.id in ids }
}

fun agruparSuscripciones(egresos: List<Egreso>, avisos: List<Recordatorio>): List<Suscripcion> {
    val porEgreso = avisos.associateBy { it.expenseId }
    return egresos.filter { it.categoryId == SUBSCRIPTIONS_ID }.groupBy { it.claveSuscripcion() }
        .values.map { cobros -> Suscripcion(cobros, cobros.mapNotNull { porEgreso[it.id] }) }
}
