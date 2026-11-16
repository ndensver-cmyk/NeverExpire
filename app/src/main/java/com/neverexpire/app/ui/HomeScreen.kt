package com.neverexpire.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neverexpire.app.data.CATEGORIES
import com.neverexpire.app.data.ExpiryStatus
import com.neverexpire.app.data.Product
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    vm: InventoryViewModel,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onTab: (Tab) -> Unit
) {
    val products by vm.products.collectAsStateWithLifecycle()
    val all by vm.all.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val cat by vm.categoryFilter.collectAsStateWithLifecycle()
    val st by vm.statusFilter.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val counts = ExpiryStatus.entries.associateWith { s -> all.count { it.status(soonDays = settings.alertDays) == s } }
    val consumeFirst = all.filter { it.daysLeft() in 0..settings.alertDays.toLong() }.sortedBy { it.expiryEpochDay }

    fun deleteWithUndo(p: Product) {
        vm.delete(p.id)
        scope.launch {
            val r = snackbar.showSnackbar("${p.name} eliminado", actionLabel = "Deshacer", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) vm.restore(p)
        }
    }

    Scaffold(
        bottomBar = { MainBottomBar(Tab.HOME, onTab) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = Color.White) { Icon(Icons.Default.Add, "Agregar manualmente") }
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Header(all.size, counts, st) { vm.statusFilter.value = if (st == it) null else it } }

            if (consumeFirst.isNotEmpty()) {
                item {
                    Text("Consume primero", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 20.dp))
                }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(consumeFirst, key = { "cf${it.id}" }) { p -> ConsumeFirstCard(p) { onOpen(p.id) } }
                    }
                }
            }

            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = query, onValueChange = { vm.query.value = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Buscar por nombre o marca") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Default.Close, "Borrar") }
                        },
                        shape = RoundedCornerShape(28.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(cat == null, { vm.categoryFilter.value = null }, { Text("Todas") }) }
                        items(CATEGORIES.filter { c -> all.any { it.category == c } }) { c ->
                            FilterChip(cat == c, { vm.categoryFilter.value = if (cat == c) null else c }, { Text(c) })
                        }
                    }
                }
            }

            if (products.isEmpty()) {
                item { EmptyState(all.isEmpty(), onScan = { onTab(Tab.SCAN) }, onAdd = onAdd) }
            }

            items(products, key = { it.id }) { p ->
                SwipeRow(
                    p = p, soonDays = settings.alertDays,
                    onOpen = { onOpen(p.id) },
                    onConsume = { vm.changeQty(p.id, -1) },
                    onDelete = { deleteWithUndo(p) }
                )
            }
        }
    }
}

@Composable
private fun Header(total: Int, counts: Map<ExpiryStatus, Int>, selected: ExpiryStatus?, onFilter: (ExpiryStatus) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.82f))))
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("NeverExpire", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Surface(color = Color.White.copy(alpha = 0.18f), shape = RoundedCornerShape(50)) {
                Text("3.1 SMART", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            if (total == 0) "Tu despensa está vacía · búsqueda inteligente" else "$total productos · búsqueda inteligente",
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpiryStatus.entries.forEach { s ->
                val bg by animateColorAsState(
                    if (selected == s) Color.White else Color.White.copy(alpha = 0.14f), label = "bg"
                )
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(bg)
                        .clickable { onFilter(s) }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${counts[s] ?: 0}", style = MaterialTheme.typography.titleLarge,
                        color = if (selected == s) statusColor(s) else Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor(s)))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            s.label, style = MaterialTheme.typography.labelSmall,
                            color = if (selected == s) Color.DarkGray else Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsumeFirstCard(p: Product, onClick: () -> Unit) {
    val color = statusColor(p.status())
    Card(
        Modifier.width(150.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(10.dp)) {
            ProductImage(p.imageUrl, p.name, p.category, 130.dp, corner = 12.dp)
            Spacer(Modifier.height(8.dp))
            Text(p.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            StatusPill(shortDays(p.daysLeft()), color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRow(p: Product, soonDays: Int, onOpen: () -> Unit, onConsume: () -> Unit, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = {
        when (it) {
            SwipeToDismissBoxValue.StartToEnd -> { onConsume(); false }
            SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
            else -> false
        }
    })
    SwipeToDismissBox(
        state = state,
        modifier = Modifier.padding(horizontal = 16.dp),
        backgroundContent = {
            val toRight = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    .background(if (toRight) Color(0xFF2E9E4F) else Color(0xFFD32F2F))
                    .padding(horizontal = 20.dp),
                contentAlignment = if (toRight) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (toRight) Icons.Default.Check else Icons.Default.Delete, null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(if (toRight) "Consumir 1" else "Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) {
        ProductCard(p, soonDays, onOpen)
    }
}

@Composable
private fun ProductCard(p: Product, soonDays: Int, onClick: () -> Unit) {
    val status = p.status(soonDays = soonDays)
    val color = statusColor(status)
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductImage(p.imageUrl, p.name, p.category, 60.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = listOf(p.brand, p.size).filter { it.isNotBlank() }.joinToString(" · ")
                Text(
                    if (sub.isNotBlank()) "$sub · x${p.quantity}" else "${p.category} · x${p.quantity}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { p.lifeProgress() },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = color, trackColor = color.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${daysText(p.daysLeft())} · ${p.expiryDate.format(dateFmt)}",
                    style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmptyState(empty: Boolean, onScan: () -> Unit, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (empty) "Aún no tienes productos" else "Sin resultados", style = MaterialTheme.typography.titleLarge)
        Text(
            if (empty) "Escanea el código de barras o la fecha del empaque, y NeverExpire completa el resto."
            else "Prueba con otra búsqueda o quita los filtros.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (empty) {
            Button(onClick = onScan) { Text("Escanear un producto") }
            TextButton(onClick = onAdd) { Text("Agregar a mano") }
        }
    }
}
