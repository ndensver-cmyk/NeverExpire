package com.neverexpire.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neverexpire.app.data.CATEGORIES
import com.neverexpire.app.data.Product
import com.neverexpire.app.data.guessCategory
import com.neverexpire.app.net.ProductInfo
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private fun Product.withInfo(i: ProductInfo) = copy(
    name = i.name.ifBlank { name },
    brand = i.brand.ifBlank { brand },
    imageUrl = i.imageUrl.ifBlank { imageUrl },
    size = i.size.ifBlank { size },
    category = if (i.category != "Otros") i.category else guessCategory(emptyList(), "${i.name} ${i.brand} ${i.ingredients}"),
    description = i.description.ifBlank { description },
    ingredients = i.ingredients.ifBlank { ingredients },
    allergens = i.allergens.ifBlank { allergens },
    labels = i.labels.ifBlank { labels },
    nutriScore = i.nutriScore.ifBlank { nutriScore },
    novaGroup = if (i.novaGroup > 0) i.novaGroup else novaGroup,
    nutrients = i.nutrients.ifEmpty { nutrients },
    barcode = i.barcode.ifBlank { barcode }
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(vm: InventoryViewModel, productId: Long?, fromScan: Boolean, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    val draft = remember { if (fromScan) vm.scanDraft.value else null }
    var p by remember {
        mutableStateOf(
            productId?.let { vm.find(it) } ?: Product(
                id = 0, name = "", category = "Otros", quantity = 1,
                expiryEpochDay = (draft?.expiry ?: LocalDate.now().plusDays(7)).toEpochDay(),
                barcode = draft?.barcode.orEmpty(),
                ingredients = draft?.ingredients.orEmpty(),
                packageText = draft?.packageText.orEmpty()
            )
        )
    }
    var expiryConfirmed by remember { mutableStateOf(!fromScan || draft?.expiryAutoConfirmed == true) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<ProductInfo>?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var catOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showMore by remember { mutableStateOf(false) }

    fun matchScore(query: String, candidate: String): Double {
        val q = query.lowercase().split(Regex("[^a-z0-9áéíóúñ]+")).filter { it.length > 2 }.toSet()
        val c = candidate.lowercase().split(Regex("[^a-z0-9áéíóúñ]+")).filter { it.length > 2 }.toSet()
        if (q.isEmpty() || c.isEmpty()) return 0.0
        return q.intersect(c).size.toDouble() / q.size.toDouble()
    }

    fun lookupCode(code: String) {
        if (code.isBlank()) return
        scope.launch {
            loading = true; message = null
            val info = vm.lookupBarcode(code)
            loading = false
            if (info != null && info.name.isNotBlank()) {
                p = p.withInfo(info)
                message = "Producto identificado automáticamente"
                if (info.imageUrl.isBlank() || info.category == "Otros" || info.description.isBlank()) {
                    val query = listOf(info.name, info.brand).filter { it.isNotBlank() }.joinToString(" ")
                    if (query.isNotBlank()) {
                        val alternatives = vm.searchByName(query)
                        val better = alternatives.maxByOrNull { matchScore(query, it.name) }
                        if (better != null && matchScore(query, better.name) >= 0.6) p = p.withInfo(better)
                    }
                }
            } else {
                val hint = draft?.nameHint.orEmpty()
                if (hint.isNotBlank()) {
                    val alternatives = vm.searchByName(hint)
                    val best = alternatives.maxByOrNull { matchScore(hint, it.name) }
                    if (best != null && matchScore(hint, best.name) >= 0.55) {
                        p = p.withInfo(best)
                        message = "Encontramos el producto por el texto del empaque"
                    } else {
                        p = p.copy(name = hint, category = guessCategory(emptyList(), hint))
                        results = alternatives
                        searchText = hint
                        message = if (alternatives.isEmpty()) "No encontramos una ficha automática. Puedes completar solo lo que falte." else "Elige el resultado que coincida con tu producto."
                    }
                } else {
                    message = "No encontramos este código. Puedes buscar el producto por su nombre."
                }
            }
        }
    }

    fun searchName(q: String) {
        if (q.isBlank()) return
        scope.launch {
            loading = true; message = null
            val r = vm.searchByName(q)
            loading = false
            val best = r.maxByOrNull { matchScore(q, it.name) }
            if (best != null && matchScore(q, best.name) >= 0.65) {
                p = p.withInfo(best)
                message = "Información del producto cargada automáticamente"
            } else if (r.isEmpty()) message = "Sin resultados para \"$q\"" else results = r
        }
    }

    LaunchedEffect(Unit) {
        if (draft != null) {
            vm.scanDraft.value = null
            if (!draft.barcode.isNullOrBlank()) lookupCode(draft.barcode)
            else if (!draft.nameHint.isNullOrBlank()) {
                p = p.copy(name = draft.nameHint, category = guessCategory(emptyList(), draft.nameHint))
                searchName(draft.nameHint)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == null) "Nuevo producto" else "Editar producto") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { onSaved(vm.save(p.copy(name = p.name.trim())).id) },
                    enabled = p.name.isNotBlank() && expiryConfirmed,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp)
                ) { Text("Guardar en mi despensa") }
            }
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductImage(p.imageUrl, p.name.ifBlank { "?" }, p.category, 96.dp, corner = 20.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p.name.ifBlank { "Producto sin nombre" }, style = MaterialTheme.typography.titleLarge,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    FilledTonalButton(onClick = {
                        if (p.barcode.isNotBlank()) lookupCode(p.barcode) else { searchText = p.name; searchName(p.name) }
                    }, enabled = !loading && (p.barcode.isNotBlank() || p.name.isNotBlank())) {
                        Icon(Icons.Default.CloudDownload, null); Spacer(Modifier.width(6.dp)); Text("Buscar imagen e info")
                    }
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }

            OutlinedTextField(
                value = p.name, onValueChange = { p = p.copy(name = it) },
                label = { Text("Nombre del producto") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { searchText = p.name; searchName(p.name) }, enabled = p.name.isNotBlank()) {
                        Icon(Icons.Default.Search, "Buscar en internet")
                    }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(p.brand, { p = p.copy(brand = it) }, Modifier.weight(1f),
                    label = { Text("Marca") }, singleLine = true)
                OutlinedTextField(p.size, { p = p.copy(size = it) }, Modifier.weight(1f),
                    label = { Text("Presentación") }, singleLine = true, placeholder = { Text("1 L, 500 g") })
            }

            ExposedDropdownMenuBox(expanded = catOpen, onExpandedChange = { catOpen = it }) {
                OutlinedTextField(
                    value = p.category, onValueChange = {}, readOnly = true, label = { Text("Categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catOpen) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = catOpen, onDismissRequest = { catOpen = false }) {
                    CATEGORIES.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { p = p.copy(category = c); catOpen = false }) }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cantidad", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = { p = p.copy(quantity = (p.quantity - 1).coerceAtLeast(1)) }) {
                    Icon(Icons.Default.Remove, "Menos")
                }
                Text("${p.quantity}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
                FilledTonalIconButton(onClick = { p = p.copy(quantity = p.quantity + 1) }) { Icon(Icons.Default.Add, "Más") }
            }

            SectionCard("Fecha de vencimiento") {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(8.dp))
                    Text("${p.expiryDate.format(dateFmt)} · ${daysText(p.daysLeft()).lowercase()}")
                }
                if (!expiryConfirmed) {
                    Text(
                        if (draft?.dateOptions.isNullOrEmpty()) "No se detectó una fecha fiable. Selecciónala manualmente."
                        else "NeverExpire leyó posibles fechas. Confirma cuál corresponde al vencimiento.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!draft?.dateOptions.isNullOrEmpty()) {
                    Text("Fechas leídas del empaque · toca para confirmar", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        draft!!.dateOptions.forEach { d ->
                            FilterChip(selected = p.expiryEpochDay == d.toEpochDay(),
                                onClick = { p = p.copy(expiryEpochDay = d.toEpochDay()); expiryConfirmed = true },
                                label = { Text(d.format(dateFmt)) })
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3L to "+3 días", 7L to "+1 semana", 30L to "+1 mes", 180L to "+6 meses").forEach { (d, l) ->
                        AssistChip(onClick = { p = p.copy(expiryEpochDay = LocalDate.now().plusDays(d).toEpochDay()); expiryConfirmed = true },
                            label = { Text(l, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }

            OutlinedTextField(
                value = p.barcode, onValueChange = { v -> p = p.copy(barcode = v.filter { it.isDigit() }) },
                label = { Text("Código de barras") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(onClick = { lookupCode(p.barcode) }, enabled = p.barcode.length >= 8) {
                        Icon(Icons.Default.CloudDownload, "Buscar por código")
                    }
                }
            )
            TextButton(onClick = { showMore = !showMore }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showMore) "Ocultar información adicional" else "Más información (opcional)")
            }
            if (showMore) {
                OutlinedTextField(p.description, { p = p.copy(description = it) }, Modifier.fillMaxWidth(),
                    label = { Text("Descripción") }, minLines = 2)
                OutlinedTextField(p.ingredients, { p = p.copy(ingredients = it) }, Modifier.fillMaxWidth(),
                    label = { Text("Ingredientes") }, minLines = 2)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = p.expiryDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        p = p.copy(expiryEpochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay())
                        expiryConfirmed = true
                    }
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state) }
    }

    results?.let { list ->
        ModalBottomSheet(onDismissRequest = { results = null }) {
            Text("Resultados para \"$searchText\"", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(list) { info ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            p = p.withInfo(info); results = null; message = "Información cargada: ${info.name}"
                        }.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductImage(info.imageUrl, info.name, info.category, 56.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(info.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(listOf(info.brand, info.size).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (info.nutriScore.isNotBlank()) {
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)) { Text(info.nutriScore) }
                        }
                    }
                }
            }
        }
    }
}
