package com.neverexpire.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.neverexpire.app.scan.FrameResult
import com.neverexpire.app.scan.PackageAnalyzer
import com.neverexpire.app.scan.PackageTextParser
import com.neverexpire.app.scan.ScanDraft
import java.time.LocalDate
import java.util.concurrent.Executors

/** Acumula lo que se va leyendo en varios cuadros para tener un resultado estable. */
private class ScanAccumulator {
    var barcode by mutableStateOf<String?>(null)
    var dateScores = mutableStateMapOf<LocalDate, Int>()
    var dateBestScores = mutableStateMapOf<LocalDate, Int>()
    var ingredients by mutableStateOf("")
    var longestText by mutableStateOf("")
    var nameHint by mutableStateOf<String?>(null)
    var wordsRead by mutableIntStateOf(0)

    fun add(r: FrameResult) {
        val code = r.barcode?.takeIf { it.length >= 8 } ?: PackageTextParser.findBarcode(r.text)
        if (barcode == null && code != null) barcode = code
        if (r.text.isNotBlank()) {
            PackageTextParser.findDates(r.text).forEach { c ->
                dateScores[c.date] = (dateScores[c.date] ?: 0) + c.score
                dateBestScores[c.date] = maxOf(dateBestScores[c.date] ?: Int.MIN_VALUE, c.score)
            }
            val ing = PackageTextParser.findIngredients(r.text)
            if (ing.length > ingredients.length) ingredients = ing
            if (r.text.length > longestText.length) longestText = r.text
            wordsRead = maxOf(wordsRead, r.text.split(Regex("\\s+")).size)
        }
        if (nameHint == null) nameHint = usableNameHint(r.biggestLine)
    }

    fun bestDates(): List<LocalDate> = dateScores.entries.sortedByDescending { it.value }.map { it.key }.take(4)

    fun hasConfidentExpiry(): Boolean = false

    private fun usableNameHint(line: String?): String? {
        val value = line?.trim()?.replace(Regex("\\s+"), " ") ?: return null
        if (value.length !in 4..32) return null
        if (Regex("\\d{2,}").containsMatchIn(value)) return null
        val letters = value.count { it.isLetter() }
        if (letters < 4 || letters.toDouble() / value.length < 0.75) return null
        val ignored = listOf("LOTE", "VENCE", "VENCIMIENTO", "INGREDIENTES", "REGISTRO", "CONTENIDO NETO", "FECHA")
        if (ignored.any { value.uppercase().contains(it) }) return null
        return value
    }

    fun reset() {
        barcode = null; dateScores.clear(); dateBestScores.clear(); ingredients = ""; longestText = ""; nameHint = null; wordsRead = 0
    }

    fun toDraft() = ScanDraft(
        barcode = barcode, expiry = bestDates().firstOrNull(), dateOptions = bestDates(),
        expiryAutoConfirmed = hasConfidentExpiry(), nameHint = nameHint, ingredients = ingredients, packageText = longestText.take(2000)
    )
}

@Composable
fun ScanScreen(vm: InventoryViewModel, onDone: () -> Unit, onTab: (Tab) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    Scaffold(bottomBar = { MainBottomBar(Tab.SCAN, onTab) }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(Color.Black)) {
            if (granted) CameraContent(vm, onDone)
            else Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("NeverExpire necesita la cámara para leer el empaque.", color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Permitir cámara") }
            }
        }
    }
}

@Composable
private fun CameraContent(vm: InventoryViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val acc = remember { ScanAccumulator() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torch by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val analyzer = PackageAnalyzer { acc.add(it) }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder().setResolutionStrategy(
                        ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                    ).build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also { it.setAnalyzer(executor, analyzer) }
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (_: Exception) { }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { future.get().unbindAll() }
            analyzer.close()
            executor.shutdown()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView({ previewView }, Modifier.fillMaxSize())

        // Marco guía
        Box(
            Modifier.align(Alignment.Center).fillMaxWidth(0.85f).aspectRatio(1.4f)
                .border(3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
        )

        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Apunta al código de barras y luego a la fecha de vencimiento",
                    color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { torch = !torch; camera?.cameraControl?.enableTorch(torch) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f))
                ) { Icon(if (torch) Icons.Default.FlashOn else Icons.Default.FlashOff, "Linterna", tint = Color.White) }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val dates = acc.bestDates()
            DetectRow(Icons.Default.QrCode, "Código de barras", acc.barcode ?: "Buscando…", acc.barcode != null)
            DetectRow(
                Icons.Default.CalendarMonth, "Fecha de vencimiento",
                dates.firstOrNull()?.format(dateFmt) ?: "Buscando…", dates.isNotEmpty()
            )
            DetectRow(
                Icons.Default.TextFields, "Texto del empaque",
                if (acc.wordsRead > 0) "${acc.wordsRead} palabras leídas" else "Buscando…", acc.wordsRead > 5
            )
            AnimatedVisibility(acc.ingredients.isNotBlank()) {
                Text("Ingredientes detectados", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { acc.reset() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reiniciar")
                }
                Button(
                    onClick = { vm.scanDraft.value = acc.toDraft(); onDone() },
                    modifier = Modifier.weight(1.4f)
                ) { Text(if (acc.barcode != null || dates.isNotEmpty()) "Continuar" else "Agregar a mano") }
            }
        }
    }
}

@Composable
private fun DetectRow(icon: ImageVector, title: String, value: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (ok) Icons.Default.CheckCircle else icon, null,
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
