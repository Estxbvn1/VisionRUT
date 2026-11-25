package cl.visionrut.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object VisionRUTIcons {
    private fun outline(name: String, stroke: Float = 1.2f, draw: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = stroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathBuilder = draw,
                )
            }
            .build()


    val Perfume = outline("Perfume", 1.6f) {
        moveTo(9f, 3f)
        lineTo(15f, 3f)
        lineTo(15f, 6f)
        lineTo(9f, 6f)
        close()
        moveTo(11f, 6f)
        lineTo(11f, 9f)
        moveTo(13f, 6f)
        lineTo(13f, 9f)
        moveTo(8f, 9f)
        lineTo(16f, 9f)
        curveTo(18f, 9f, 19f, 11f, 19f, 13f)
        lineTo(19f, 20f)
        lineTo(5f, 20f)
        lineTo(5f, 13f)
        curveTo(5f, 11f, 6f, 9f, 8f, 9f)
        close()
        moveTo(9f, 13f)
        lineTo(15f, 13f)
        lineTo(15f, 16f)
        lineTo(9f, 16f)
        close()
    }

    val Zapatilla = outline("Zapatilla", 1.6f) {
        moveTo(3f, 10f)
        lineTo(6f, 11f)
        lineTo(9f, 8f)
        lineTo(14f, 13f)
        curveTo(16f, 15f, 19f, 14f, 21f, 16f)
        lineTo(21f, 19f)
        lineTo(3f, 19f)
        close()
        moveTo(3f, 16f)
        lineTo(12f, 16f)
        lineTo(15f, 17f)
        lineTo(21f, 17f)
        moveTo(11f, 10f)
        lineTo(9f, 12f)
        moveTo(13f, 12f)
        lineTo(11f, 14f)
    }

    val Polera = outline("Polera", 1.6f) {
        moveTo(8f, 4f)
        curveTo(9f, 8f, 15f, 8f, 16f, 4f)
        lineTo(21f, 7f)
        lineTo(19f, 12f)
        lineTo(16f, 11f)
        lineTo(16f, 21f)
        lineTo(8f, 21f)
        lineTo(8f, 11f)
        lineTo(5f, 12f)
        lineTo(3f, 7f)
        close()
    }

    val Search =
        outline("VisionRUTSearch") {
            moveTo(17f, 10f)
            curveTo(17f, 19.3f, 3f, 19.3f, 3f, 10f)
            curveTo(3f, .7f, 17f, .7f, 17f, 10f)
            close()
            moveTo(15f, 15f)
            lineTo(21f, 21f)
        }
    val Bell =
        outline("VisionRUTBell") {
            moveTo(3f, 19f)
            curveTo(5.3f, 17.7f, 5.5f, 15.8f, 5.5f, 10f)
            curveTo(5.5f, 6.5f, 7.5f, 4.7f, 10.5f, 4f)
            lineTo(10.5f, 2.7f)
            curveTo(10.5f, 1.2f, 13.5f, 1.2f, 13.5f, 2.7f)
            lineTo(13.5f, 4f)
            curveTo(16.5f, 4.7f, 18.5f, 6.5f, 18.5f, 10f)
            curveTo(18.5f, 15.8f, 18.7f, 17.7f, 21f, 19f)
            close()
            moveTo(10f, 21f)
            curveTo(11f, 23f, 13f, 23f, 14f, 21f)
        }
    val Menu =
        outline("VisionRUTMenu") {
            moveTo(3f, 5f)
            lineTo(21f, 5f)
            moveTo(3f, 12f)
            lineTo(21f, 12f)
            moveTo(3f, 19f)
            lineTo(21f, 19f)
        }
    val Home =
        outline("InicioScreen") {
            moveTo(1.5f, 10f)
            lineTo(12f, 1f)
            lineTo(22.5f, 10f)
            moveTo(4.5f, 10f)
            lineTo(4.5f, 22f)
            lineTo(19.5f, 22f)
            lineTo(19.5f, 10f)
            moveTo(10f, 22f)
            lineTo(10f, 16f)
            lineTo(14f, 16f)
            lineTo(14f, 22f)
        }
    val Transfer =
        outline("VisionRUTTransfer", 1.45f) {
            moveTo(4f, 6f)
            lineTo(21f, 6f)
            moveTo(16.5f, 1.5f)
            lineTo(21f, 6f)
            lineTo(16.5f, 10.5f)
            moveTo(20f, 18f)
            lineTo(3f, 18f)
            moveTo(7.5f, 13.5f)
            lineTo(3f, 18f)
            lineTo(7.5f, 22.5f)
        }
    val Security =
        outline("VisionRUTSecurity", 1.45f) {
            moveTo(7f, 9f)
            lineTo(7f, 6f)
            curveTo(7f, -1f, 17f, -1f, 17f, 6f)
            lineTo(17f, 9f)
            moveTo(5f, 9f)
            lineTo(19f, 9f)
            curveTo(20f, 9f, 21f, 10f, 21f, 11f)
            lineTo(21f, 21f)
            curveTo(21f, 22f, 20f, 23f, 19f, 23f)
            lineTo(5f, 23f)
            curveTo(4f, 23f, 3f, 22f, 3f, 21f)
            lineTo(3f, 11f)
            curveTo(3f, 10f, 4f, 9f, 5f, 9f)
            close()
            moveTo(12f, 12f)
            curveTo(8.8f, 12f, 8.8f, 16f, 11f, 16.4f)
            lineTo(11f, 19f)
            lineTo(13f, 19f)
            lineTo(13f, 16.4f)
            curveTo(15.2f, 16f, 15.2f, 12f, 12f, 12f)
            close()
        }
    val More =
        outline("VisionRUTMore", 1.35f) {
            for (row in 0..2) for (column in 0..2) {
                val x = 2f + column * 7.5f
                val y = 2f + row * 7.5f
                moveTo(x, y)
                lineTo(x + 5f, y)
                lineTo(x + 5f, y + 5f)
                lineTo(x, y + 5f)
                close()
            }
        }

    private fun eye(hidden: Boolean) =
        outline(if (hidden) "VisionRUTEyeOff" else "VisionRUTEye") {
            moveTo(1f, 12f)
            curveTo(7f, 3f, 17f, 3f, 23f, 12f)
            curveTo(17f, 21f, 7f, 21f, 1f, 12f)
            close()
            moveTo(16f, 12f)
            curveTo(16f, 17.3f, 8f, 17.3f, 8f, 12f)
            curveTo(8f, 6.7f, 16f, 6.7f, 16f, 12f)
            close()
            if (hidden) {
                moveTo(3f, 22f)
                lineTo(21f, 2f)
            }
        }

    val Eye = eye(false)
    val EyeOff = eye(true)
    val Toll =
        outline("AutopistaTAG", 1.6f) {
            moveTo(3f, 21f)
            lineTo(6f, 9f)
            lineTo(18f, 9f)
            lineTo(21f, 21f)
            moveTo(12f, 12f)
            lineTo(12f, 14f)
            moveTo(12f, 18f)
            lineTo(12f, 21f)
            moveTo(4f, 12f)
            lineTo(4f, 5f)
            lineTo(20f, 5f)
            lineTo(20f, 12f)
            moveTo(8f, 2f)
            lineTo(16f, 2f)
            moveTo(8f, 7f)
            lineTo(16f, 7f)
        }
}

fun iconoCategoria(key: String): ImageVector =
    when (key) {
        "toll" -> VisionRUTIcons.Toll
        "transfers" -> VisionRUTIcons.Transfer
        "home" -> Icons.Outlined.Home
        "car" -> Icons.Outlined.DirectionsCar
        "work" -> Icons.Outlined.WorkOutline
        "travel" -> Icons.Outlined.Flight
        "gift" -> Icons.Outlined.CardGiftcard
        "tools" -> Icons.Outlined.Build
        "beauty" -> Icons.Outlined.Spa
        "food" -> Icons.Outlined.Restaurant
        "transport" -> Icons.Outlined.DirectionsBus
        "bills" -> Icons.AutoMirrored.Outlined.ReceiptLong
        "perfume" -> VisionRUTIcons.Perfume
        "sneakers" -> VisionRUTIcons.Zapatilla
        "clothes" -> VisionRUTIcons.Polera
        "motorcycle" -> Icons.Outlined.TwoWheeler
        "delivery" -> Icons.Outlined.DeliveryDining
        "shopping" -> Icons.Outlined.ShoppingBag
        "fun" -> Icons.Outlined.SportsEsports
        "subscriptions" -> Icons.Outlined.LiveTv
        "pets" -> Icons.Outlined.Pets
        "health" -> Icons.Outlined.Add
        "study" -> Icons.Outlined.School
        "heart" -> Icons.Outlined.FavoriteBorder
        "sport" -> Icons.Outlined.FitnessCenter
        else -> Icons.Outlined.MoreHoriz
    }
