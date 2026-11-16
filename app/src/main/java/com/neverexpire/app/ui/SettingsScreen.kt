package com.neverexpire.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: InventoryViewModel, onTab: (Tab) -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val all by vm.all.collectAsStateWithLifecycle()
    val expired = all.count { it.daysLeft() < 0 }
    var days by remember(s) { mutableFloatStateOf(s.alertDays.toFloat()) }
    var hour by remember(s) { mutableFloatStateOf(s.notifyHour.toFloat()) }
    var tested by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ajustes") }) },
        bottomBar = { MainBottomBar(Tab.SETTINGS, onTab) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard("Avisos") {
                Text("Avisarme cuando falten ${days.toInt()} día(s) o menos")
                Slider(days, { days = it }, valueRange = 1f..10f, steps = 8,
                    onValueChangeFinished = { vm.updateSettings(s.copy(alertDays = days.toInt())) })
                Text("Hora del aviso diario: ${formatHour(hour.toInt())}")
                Slider(hour, { hour = it }, valueRange = 6f..22f, steps = 15,
                    onValueChangeFinished = { vm.updateSettings(s.copy(notifyHour = hour.toInt())) })
                OutlinedButton(onClick = { vm.testNotification(); tested = true }) { Text("Probar aviso ahora") }
                if (tested) Text("Si hay productos por vencer, verás la notificación en unos segundos.",
                    style = MaterialTheme.typography.bodySmall)
            }
            SectionCard("Despensa") {
                Text("${all.size} productos · $expired vencidos")
                Button(onClick = { vm.deleteExpired() }, enabled = expired > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Eliminar vencidos")
                }
            }
            SectionCard("Cómo funciona el escáner") {
                Text("La cámara lee el código de barras y el texto del empaque directamente en tu teléfono. " +
                    "Con el código buscamos el nombre, la imagen, los ingredientes y la información nutricional en Open Food Facts, " +
                    "una base de datos abierta y gratuita. Si el producto no está, puedes buscarlo por nombre o llenarlo a mano.")
            }
            Text("NeverExpire 2.0", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatHour(h: Int): String = when {
    h == 12 -> "12:00 p. m."
    h > 12 -> "${h - 12}:00 p. m."
    else -> "$h:00 a. m."
}
