package cl.visionrut.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.visionrut.app.R
import cl.visionrut.app.ui.components.VisionRUTIcons
import cl.visionrut.app.ui.theme.Background
import cl.visionrut.app.ui.theme.Blue
import cl.visionrut.app.ui.theme.Ink
import cl.visionrut.app.ui.theme.Muted
import cl.visionrut.app.ui.theme.Orange

/** Inicio bancario de demostración y acceso al módulo VisionRUT. */
@Composable
fun InicioScreen(open: () -> Unit, movements: () -> Unit, info: () -> Unit) {
    var balanceVisible by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        containerColor = Background,
        topBar = {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(Color(0xFFAA5D15))
                )
                Row(
                    Modifier.fillMaxWidth()
                        .height(70.dp)
                        .background(Orange)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painterResource(R.drawable.bancoestado_logo),
                        "BancoEstado",
                        Modifier.width(140.dp).height(34.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.weight(1f))
                    listOf(
                            VisionRUTIcons.Search to "Buscar",
                            VisionRUTIcons.Bell to "Notificaciones",
                            VisionRUTIcons.Menu to "Menú",
                        )
                        .forEach { (icon, description) ->
                            IconButton(
                                info,
                                Modifier.padding(start = 12.dp)
                                    .size(40.dp)
                                    .background(Color(0xFFFFF7E7), CircleShape),
                            ) {
                                Icon(icon, description, Modifier.size(25.dp), tint = Blue)
                            }
                        }
                }
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(72.dp)) {
                    listOf(
                            VisionRUTIcons.Home to "Inicio",
                            VisionRUTIcons.Transfer to "Transferir",
                            VisionRUTIcons.Security to "Seguridad",
                            VisionRUTIcons.More to "Más",
                        )
                        .forEachIndexed { index, (icon, label) ->
                            Column(
                                Modifier.weight(1f).fillMaxHeight().clickable(onClick = info),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier.fillMaxWidth()
                                        .height(2.dp)
                                        .background(if (index == 0) Orange else Color.Transparent)
                                )
                                Icon(
                                    icon,
                                    null,
                                    Modifier.padding(top = 8.dp).size(28.dp),
                                    tint = if (index == 0) Orange else Ink,
                                )
                                Text(
                                    label,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Ink,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                }
            }
        },
    ) { insets ->
        Box(Modifier.padding(insets).fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth()
                    .height(60.dp)
                    .background(Orange, RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            )
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            Modifier.heightIn(min = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "CuentaRUT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                            )
                            Text(
                                " 0000001234",
                                fontSize = 16.sp,
                           lineHeight = 20.sp,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(info, Modifier.size(24.dp)) {
                                Icon(Icons.Outlined.MoreVert, "Opciones de cuenta", tint = Blue)
                            }
                        }
                        Row(
                            Modifier.padding(top = 6.dp).heightIn(min = 34.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (balanceVisible) "$999.999.999.999" else "$*********",
                                fontSize = 22.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("bank-balance"),
                            )
                            IconButton(
                                { balanceVisible = !balanceVisible },
                                Modifier.padding(start = 8.dp).size(34.dp),
                            ) {
                                Icon(
                                    if (balanceVisible) VisionRUTIcons.Eye
                                    else VisionRUTIcons.EyeOff,
                                    if (balanceVisible) "Ocultar saldo" else "Mostrar saldo",
                                    tint = Blue,
                                    modifier = Modifier.size(25.dp),
                                )
                            }
                        }
                        Text(
                            "Saldo disponible",
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 5.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Button(
                                movements,
                                Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Text(
                                    "Movimientos",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            OutlinedButton(
                                info,
                                Modifier.weight(1f).height(32.dp),
                                border = androidx.compose.foundation.BorderStroke(1.7.dp, Blue),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Text(
                                    "Cartolas",
                                    fontSize = 14.sp,
                                    color = Blue,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Surface(
                    onClick = open,
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.testTag("bank-visionrut"),
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(16.dp)) {
                            Text(
                                "VisionRUT",
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Conoce tus egresos y\norganízalos a tu manera.",
                                fontSize = 14.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                            )
                            Box(
                                Modifier.background(Blue, RoundedCornerShape(22.dp))
                                    .padding(horizontal = 12.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "Entrar aquí",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        Image(
                            painterResource(R.drawable.bank_promo),
                            null,
                            Modifier.width(80.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(topStart = 44.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier.fillMaxWidth().height(10.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                ) {
                    repeat(5) { index ->
                        Box(
                            Modifier.size(10.dp)
                                .background(
                                    if (index == 0) Orange else Color(0xFFD6D6D6),
                                    CircleShape,
                                )
                        )
                    }
                }
                Text("Tus tarjetas", fontSize = 18.sp, lineHeight = 24.sp)
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                ) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 110.dp)) {
                        Canvas(Modifier.width(8.dp).height(110.dp)) {
                            drawRect(Color(0xFF1026AC))
                            drawPath(
                                Path().apply {
                                    moveTo(0f, size.height * .55f)
                                    lineTo(size.width, size.height * .43f)
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                },
                                Color(0xFFFF1744),
                            )
                        }
                        Column(Modifier.weight(1f).padding(start = 8.dp, top = 12.dp)) {
                            Text(
                                "Tarjeta CuentaRUT",
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "**** **** **** 1234",
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        Image(
                            painterResource(R.drawable.bank_visa),
                            "Visa",
                            Modifier.padding(top = 20.dp, end = 10.dp).width(46.dp).height(20.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Spacer(Modifier.height(58.dp))
                Text("¿Qué necesitas hacer?", fontSize = 18.sp, lineHeight = 24.sp)
                Spacer(Modifier.height(6.dp))
                Surface(
                    onClick = movements,
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ReceiptLong,
                            null,
                            Modifier.size(28.dp),
                            tint = Orange,
                        )
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                "Revisar mis movimientos",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "Consulta y categoriza tus egresos",
                                fontSize = 13.sp,
                                color = Muted,
                            )
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Blue)
                    }
                }
                Text(
                    "Demostración académica · Datos simulados",
                    fontSize = 12.sp,
                    color = Muted,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            }
        }
    }
}
