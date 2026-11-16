package com.neverexpire.app.scan

import java.time.LocalDate

/** Lo que se detectó al escanear, para prellenar el formulario. */
data class ScanDraft(
    val barcode: String? = null,
    val expiry: LocalDate? = null,
    val dateOptions: List<LocalDate> = emptyList(),
    val expiryAutoConfirmed: Boolean = false,
    val nameHint: String? = null,
    val ingredients: String = "",
    val packageText: String = ""
)
