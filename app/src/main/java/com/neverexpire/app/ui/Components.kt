package com.neverexpire.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.neverexpire.app.data.ExpiryStatus
import java.time.format.DateTimeFormatter
import java.util.Locale

val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "CO"))

fun statusColor(s: ExpiryStatus): Color = when (s) {
    ExpiryStatus.EXPIRED -> Color(0xFFD32F2F)
    ExpiryStatus.URGENT -> Color(0xFFEF6C00)
    ExpiryStatus.SOON -> Color(0xFFF2A900)
    ExpiryStatus.OK -> Color(0xFF2E9E4F)
}

fun daysText(d: Long): String = when {
    d < -1 -> "Venció hace ${-d} días"
    d == -1L -> "Venció ayer"
    d == 0L -> "Vence hoy"
    d == 1L -> "Vence mañana"
    else -> "Vence en $d días"
}

fun shortDays(d: Long): String = when {
    d < 0 -> "Vencido"
    d == 0L -> "Hoy"
    d == 1L -> "Mañana"
    else -> "$d días"
}

fun categoryColor(c: String): Color = when (c) {
    "Lácteos" -> Color(0xFF5C8DD6); "Carnes" -> Color(0xFFC0504D); "Frutas" -> Color(0xFFE88A2D)
    "Verduras" -> Color(0xFF3E9B57); "Panadería" -> Color(0xFFB07B3E); "Enlatados" -> Color(0xFF7A7F87)
    "Bebidas" -> Color(0xFF2B9DB0); "Congelados" -> Color(0xFF5DA9E9); "Granos" -> Color(0xFFA68A3A)
    "Snacks" -> Color(0xFF9B5DB0); "Salsas" -> Color(0xFFD9534F); else -> Color(0xFF6B7B6E)
}

/** Imagen del producto con respaldo: inicial sobre el color de su categoría. */
@Composable
fun ProductImage(url: String, name: String, category: String, size: Dp, modifier: Modifier = Modifier, corner: Dp = 14.dp) {
    val shape = RoundedCornerShape(corner)
    val fallback: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxSize().background(categoryColor(category).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.trim().take(1).uppercase().ifEmpty { "?" },
                color = categoryColor(category), fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
    Box(modifier.size(size).clip(shape).background(Color.White)) {
        if (url.isBlank()) fallback()
        else SubcomposeAsyncImage(
            model = url, contentDescription = name, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) },
            error = { fallback() }
        )
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(CircleShape).background(color.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NutriScoreBadge(grade: String) {
    val colors = mapOf(
        "A" to Color(0xFF038141), "B" to Color(0xFF85BB2F), "C" to Color(0xFFFECB02),
        "D" to Color(0xFFEE8100), "E" to Color(0xFFE63E11)
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Nutri-Score ", style = MaterialTheme.typography.labelLarge)
        listOf("A", "B", "C", "D", "E").forEach { g ->
            val sel = g == grade
            Box(
                Modifier.padding(horizontal = 1.dp).size(if (sel) 30.dp else 22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.getValue(g).copy(alpha = if (sel) 1f else 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(g, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (sel) 16.sp else 12.sp)
            }
        }
    }
}

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

enum class Tab(val route: String, val label: String) {
    HOME("home", "Despensa"), SCAN("scan", "Escanear"), SETTINGS("settings", "Ajustes")
}

@Composable
fun MainBottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar {
        Tab.entries.forEach { t ->
            NavigationBarItem(
                selected = t == current,
                onClick = { if (t != current) onSelect(t) },
                icon = {
                    Icon(
                        when (t) {
                            Tab.HOME -> Icons.Filled.Inventory2
                            Tab.SCAN -> Icons.Filled.QrCodeScanner
                            Tab.SETTINGS -> Icons.Filled.Settings
                        }, null
                    )
                },
                label = { Text(t.label) }
            )
        }
    }
}
