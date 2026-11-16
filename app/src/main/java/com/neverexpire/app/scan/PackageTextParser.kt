package com.neverexpire.app.scan

import java.time.LocalDate
import java.time.YearMonth

/**
 * Analiza el texto leído del empaque (OCR) para encontrar:
 * - la fecha de vencimiento (cerca de palabras como VENCE, F.V., EXP, CONSUMIR ANTES...)
 * - el texto de ingredientes
 * - un código de barras escrito en números (EAN-8/12/13)
 */
object PackageTextParser {

    private val KEYWORDS = listOf(
        "VENCE", "VENC", "VTO", "F.V", "FV", "F. V", "FECHA DE VENCIMIENTO", "EXP", "CAD",
        "CONSUMIR ANTES", "CONSUMASE ANTES", "CONSÚMASE", "CONSUMIR PREF", "BEST BEFORE", "BB", "USE BY", "CADUCIDAD"
    )
    private val PRODUCTION = listOf("F.P", "FP", "FAB", "ELAB", "PROD", "LOTE", "MFG", "PACKED", "EMPAC")

    private val MONTHS = mapOf(
        "ENE" to 1, "JAN" to 1, "FEB" to 2, "MAR" to 3, "ABR" to 4, "APR" to 4, "MAY" to 5,
        "JUN" to 6, "JUL" to 7, "AGO" to 8, "AUG" to 8, "SEP" to 9, "SET" to 9, "OCT" to 10,
        "NOV" to 11, "DIC" to 12, "DEC" to 12
    )

    private val numeric = Regex("""(?<!\d)(\d{1,2})\s?[/\-.]\s?(\d{1,2})\s?[/\-.]\s?(\d{2,4})(?!\d)""")
    private val isoDate = Regex("""(?<!\d)(20\d{2})[/\-.](\d{1,2})[/\-.](\d{1,2})(?!\d)""")
    private val textual = Regex("""(?<!\d)(\d{1,2})?\s?[/\-. ]?\s?(ENE|JAN|FEB|MAR|ABR|APR|MAY|JUN|JUL|AGO|AUG|SEP|SET|OCT|NOV|DIC|DEC)[A-Z]*\.?\s?[/\-. ]?\s?(\d{2,4})(?!\d)""")
    private val monthYear = Regex("""(?<![\d/.\-])(\d{1,2})\s?[/\-.]\s?(20\d{2})(?!\d)""")
    private val barcode = Regex("""(?<!\d)(\d{13}|\d{12}|\d{8})(?!\d)""")

    data class DateCandidate(val date: LocalDate, val score: Int)

    fun findDates(text: String, today: LocalDate = LocalDate.now()): List<DateCandidate> {
        val upper = text.uppercase()
        val found = mutableMapOf<LocalDate, Int>()

        fun add(date: LocalDate?, start: Int, end: Int = start) {
            if (date == null) return
            if (date.isBefore(today.minusYears(2)) || date.isAfter(today.plusYears(10))) return
            var score = 1
            // Las impresiones suelen colocar VENCE/EXP antes de la fecha, aunque OCR
            // también puede devolver la etiqueta después o en la línea siguiente.
            val before = upper.substring((start - 45).coerceAtLeast(0), start)
            val after = upper.substring(end, (end + 24).coerceAtMost(upper.length))
            if (KEYWORDS.any { it in before || it in after }) score += 12
            if (PRODUCTION.any { it in before.takeLast(18) }) score -= 8
            if (!date.isBefore(today)) score += 2
            found[date] = maxOf(found[date] ?: Int.MIN_VALUE, score)
        }

        numeric.findAll(upper).forEach { m ->
            val (first, second, y) = m.destructured
            // NeverExpire está orientada al mercado hispanohablante: primero DD/MM/AAAA.
            // Solo se usa MM/DD como alternativa si la lectura día/mes no es válida.
            val parsedYear = year(y)
            val dayFirst = safeDate(parsedYear, second.toInt(), first.toInt())
            val monthFirst = safeDate(parsedYear, first.toInt(), second.toInt())
            add(dayFirst ?: monthFirst, m.range.first, m.range.last + 1)
        }
        isoDate.findAll(upper).forEach { m ->
            val (y, mo, d) = m.destructured
            add(safeDate(y.toInt(), mo.toInt(), d.toInt()), m.range.first)
        }
        textual.findAll(upper).forEach { m ->
            val (d, mon, y) = m.destructured
            val month = MONTHS[mon] ?: return@forEach
            val yy = year(y)
            val date = if (d.isNotEmpty()) safeDate(yy, month, d.toInt())
            else runCatching { YearMonth.of(yy, month).atEndOfMonth() }.getOrNull()
            add(date, m.range.first)
        }
        monthYear.findAll(upper).forEach { m ->
            val (mo, y) = m.destructured
            val yy = year(y)
            val date = runCatching { YearMonth.of(yy, mo.toInt()).atEndOfMonth() }.getOrNull()
            add(date, m.range.first, m.range.last + 1)
        }

        return found.map { DateCandidate(it.key, it.value) }
            .sortedWith(compareByDescending<DateCandidate> { it.score }.thenByDescending { it.date })
    }

    fun bestExpiry(text: String, today: LocalDate = LocalDate.now()): LocalDate? =
        findDates(text, today).firstOrNull()?.date

    fun findIngredients(text: String): String {
        val idx = text.uppercase().indexOf("INGREDIENTE")
        if (idx < 0) return ""
        val after = text.substring(idx).substringAfter(":", text.substring(idx))
        val stop = Regex("(?i)(información nutricional|informacion nutricional|tabla nutricional|contiene|registro|reg\\. ?san|invima|lote|vence|consérvese|conservese)")
        val end = stop.find(after)?.range?.first ?: after.length
        return after.substring(0, end).replace(Regex("\\s+"), " ").trim().trimEnd('.', ',').take(600)
    }

    fun findBarcode(text: String): String? =
        barcode.findAll(text.replace(" ", "")).map { it.value }.firstOrNull { isValidEan(it) }

    fun isValidEan(code: String): Boolean {
        if (code.length !in setOf(8, 12, 13) || !code.all { it.isDigit() }) return false
        val digits = code.map { it - '0' }
        val body = digits.dropLast(1).reversed()
        val sum = body.mapIndexed { i, d -> if (i % 2 == 0) d * 3 else d }.sum()
        return (10 - sum % 10) % 10 == digits.last()
    }

    private fun year(y: String): Int = if (y.length == 2) 2000 + y.toInt() else y.toInt()

    private fun safeDate(y: Int, m: Int, d: Int): LocalDate? =
        runCatching { LocalDate.of(y, m, d) }.getOrNull()
}
