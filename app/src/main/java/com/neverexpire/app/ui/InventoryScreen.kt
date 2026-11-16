package com.neverexpire.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neverexpire.app.data.CATEGORIES
import com.neverexpire.app.data.ExpiryStatus
import com.neverexpire.app.data.Product
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "CO"))

fun statusColor(s: ExpiryStatus): Color = when (s) {
    ExpiryStatus.EXPIRED -> Color(0xFFC62828)
    ExpiryStatus.URGENT -> Color(0xFFEF6C00)
    ExpiryStatus.SOON -> Color(0xFFF9A825)
    ExpiryStatus.OK -> Color(0xFF2E7D32)
}

fun daysText(d: Long): String = when {
    d < -1 -> "Venció hace ${-d} días"
    d == -1L -> "Venció ayer"
    d == 0L -> "Vence hoy"
    d == 1L -> "Vence mañana"
    else -> "Vence en $d días"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(vm: InventoryViewModel = viewModel()) {
    val products by vm.products.collectAsStateWithLifecycle()
    val all by vm.all.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val cat by vm.categoryFilter.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var menu by remember { mutableStateOf(false) }

    val expiredCount = all.count { it.status() == ExpiryStatus.EXPIRED }
    val urgent = all.filter { it.status() == ExpiryStatus.URGENT }.sortedBy { it.expiryEpochDay }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NeverExpire", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Menú") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Probar aviso ahora") },
                            onClick = { vm.testNotification(); menu = false })
                        DropdownMenuItem(
                            text = { Text("Eliminar vencidos ($expiredCount)") },
                            enabled = expiredCount > 0,
                            onClick = { vm.deleteExpired(); menu = false })
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Agregar") })
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SummaryCard(total = all.size, expired = expiredCount, urgent = urgent)
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.query.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar producto") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
            }
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = cat == null, onClick = { vm.categoryFilter.value = null },
                        label = { Text("Todos") })
                    CATEGORIES.forEach { c ->
                        FilterChip(selected = cat == c,
                            onClick = { vm.categoryFilter.value = if (cat == c) null else c },
                            label = { Text(c) })
                    }
                }
            }
            if (products.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (all.isEmpty()) "Tu despensa está vacía.\nToca \"Agregar\" para registrar un producto."
                            else "No hay resultados.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(products, key = { it.id }) { p ->
                ProductRow(
                    p = p,
                    onClick = { editing = p },
                    onConsume = { vm.consumeOne(p.id) },
                    onDelete = { vm.delete(p.id) }
                )
            }
        }
    }

    if (showAdd) {
        ProductDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { n, c, q, e -> vm.add(n, c, q, e); showAdd = false }
        )
    }
    editing?.let { p ->
        ProductDialog(
            initial = p,
            onDismiss = { editing = null },
            onSave = { n, c, q, e ->
                vm.update(p.copy(name = n, category = c, quantity = q, expiryEpochDay = e))
                editing = null
            }
        )
    }
}

@Composable
private fun SummaryCard(total: Int, expired: Int, urgent: List<Product>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Consume primero", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            if (urgent.isEmpty()) {
                Text("Nada vence en los próximos 3 días.")
            } else {
                urgent.take(3).forEach {
                    Text("• ${it.name} — ${daysText(it.daysLeft()).lowercase()}")
                }
                if (urgent.size > 3) Text("y ${urgent.size - 3} más…")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "$total productos · $expired vencidos",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ProductRow(p: Product, onClick: () -> Unit, onConsume: () -> Unit, onDelete: () -> Unit) {
    val status = p.status()
    val color = statusColor(status)
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${p.category} · x${p.quantity} · ${p.expiryDate.format(dateFmt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(daysText(p.daysLeft()), color = color,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onConsume) { Icon(Icons.Default.Check, "Consumir uno") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDialog(
    initial: Product?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Long) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: CATEGORIES.first()) }
    var qty by remember { mutableStateOf((initial?.quantity ?: 1).toString()) }
    var expiry by remember { mutableStateOf(initial?.expiryDate ?: LocalDate.now().plusDays(7)) }
    var showPicker by remember { mutableStateOf(false) }
    var catOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo producto" else "Editar producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Nombre") }, singleLine = true)
                ExposedDropdownMenuBox(expanded = catOpen, onExpandedChange = { catOpen = it }) {
                    OutlinedTextField(
                        value = category, onValueChange = {}, readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catOpen) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = catOpen, onDismissRequest = { catOpen = false }) {
                        CATEGORIES.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catOpen = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = qty, onValueChange = { v -> qty = v.filter { it.isDigit() }.take(4) },
                    label = { Text("Cantidad") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)) {
                    Text("Vence: ${expiry.format(dateFmt)}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3L to "+3d", 7L to "+1sem", 30L to "+1mes").forEach { (d, l) ->
                        AssistChip(onClick = { expiry = LocalDate.now().plusDays(d) }, label = { Text(l) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim(), category, qty.toIntOrNull() ?: 1, expiry.toEpochDay()) }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = expiry.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        expiry = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state) }
    }
}
