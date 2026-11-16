package com.neverexpire.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neverexpire.app.net.OpenFoodFacts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: InventoryViewModel, id: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val all by vm.all.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val p = all.firstOrNull { it.id == id }
    var confirmDelete by remember { mutableStateOf(false) }
    var showRaw by remember { mutableStateOf(false) }

    LaunchedEffect(p == null) { if (p == null) onBack() }
    if (p == null) return

    val status = p.status(soonDays = settings.alertDays)
    val color = statusColor(status)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") } },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Eliminar") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ProductImage(p.imageUrl, p.name, p.category, 220.dp, corner = 28.dp)
            }
            Column {
                Text(p.name, style = MaterialTheme.typography.headlineSmall)
                val sub = listOf(p.brand, p.size, p.category).filter { it.isNotBlank() }.joinToString(" · ")
                Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(daysText(p.daysLeft()), color = color, style = MaterialTheme.typography.titleLarge)
                        Text("Fecha: ${p.expiryDate.format(dateFmt)}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { p.lifeProgress() },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = color, trackColor = color.copy(alpha = 0.2f)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    StatusPill(status.label, color)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("En casa", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = { vm.changeQty(p.id, -1) }) { Icon(Icons.Default.Remove, "Consumir uno") }
                Text("${p.quantity}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
                FilledTonalIconButton(onClick = { vm.changeQty(p.id, 1) }) { Icon(Icons.Default.Add, "Agregar uno") }
            }

            if (p.description.isNotBlank()) SectionCard("Descripción") { Text(p.description) }

            if (p.nutriScore.isNotBlank() || p.novaGroup > 0) {
                SectionCard("Calidad nutricional") {
                    if (p.nutriScore.isNotBlank()) {
                        NutriScoreBadge(p.nutriScore)
                        Text(OpenFoodFacts.nutriScoreText(p.nutriScore).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (p.novaGroup > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(novaColor(p.novaGroup)),
                                contentAlignment = Alignment.Center
                            ) { Text("${p.novaGroup}", color = Color.White, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(10.dp))
                            Text("NOVA: ${OpenFoodFacts.novaText(p.novaGroup)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (p.nutrients.isNotEmpty()) {
                SectionCard("Información nutricional (por 100 g)") {
                    p.nutrients.forEachIndexed { i, n ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(n.label, Modifier.weight(1f))
                            Text(n.value, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (p.allergens.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Alérgenos", style = MaterialTheme.typography.titleMedium)
                            Text(p.allergens.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }

            if (p.ingredients.isNotBlank()) SectionCard("Ingredientes") { Text(p.ingredients) }
            if (p.labels.isNotBlank()) SectionCard("Sellos y etiquetas") { Text(p.labels) }

            SectionCard("Datos") {
                InfoLine("Categoría", p.category)
                if (p.barcode.isNotBlank()) InfoLine("Código de barras", p.barcode)
                InfoLine("Registrado", java.time.LocalDate.ofEpochDay(p.addedEpochDay).format(dateFmt))
            }

            if (p.packageText.isNotBlank()) {
                SectionCard("Texto leído del empaque") {
                    TextButton(onClick = { showRaw = !showRaw }) { Text(if (showRaw) "Ocultar" else "Mostrar") }
                    if (showRaw) Text(p.packageText, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar ${p.name}") },
            text = { Text("Se quitará de tu despensa.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(p.id) }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun novaColor(n: Int) = when (n) {
    1 -> Color(0xFF00AA00); 2 -> Color(0xFFFFCC00); 3 -> Color(0xFFFF6600); else -> Color(0xFFFF0000)
}
