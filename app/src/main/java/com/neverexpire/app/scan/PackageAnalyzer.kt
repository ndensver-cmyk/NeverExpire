package com.neverexpire.app.scan

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** Resultado de un cuadro analizado por la cámara. */
data class FrameResult(
    val barcode: String?,
    val text: String,
    val biggestLine: String?
)

/**
 * Lee cada cuadro de la cámara con ML Kit (en el teléfono, sin internet):
 * código de barras + reconocimiento de texto.
 */
class PackageAnalyzer(private val onResult: (FrameResult) -> Unit) : ImageAnalysis.Analyzer {

    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E, Barcode.FORMAT_CODE_128
            ).build()
    )
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastRun = 0L

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(proxy: ImageProxy) {
        val now = System.currentTimeMillis()
        val media = proxy.image
        if (media == null || now - lastRun < 700) { proxy.close(); return }
        lastRun = now
        val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image)
            .continueWithTask { bTask ->
                val code = (if (bTask.isSuccessful) bTask.result else null)?.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                textRecognizer.process(image).continueWith { tTask ->
                    val visionText = if (tTask.isSuccessful) tTask.result else null
                    val lines = visionText?.textBlocks?.flatMap { it.lines }.orEmpty()
                    val biggest = lines
                        .filter { l -> l.text.count { it.isLetter() } >= 3 }
                        .maxByOrNull { it.boundingBox?.height() ?: 0 }?.text
                    FrameResult(code, visionText?.text.orEmpty(), biggest)
                }
            }
            .addOnSuccessListener { onResult(it) }
            .addOnCompleteListener { proxy.close() }
    }

    fun close() {
        barcodeScanner.close()
        textRecognizer.close()
    }
}
