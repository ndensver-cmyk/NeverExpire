package com.neverexpire.app.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PackageTextParserTest {
    private val today = LocalDate.of(2026, 11, 15)

    @Test fun fechaConPalabraVence() {
        val t = "LOTE 2345 F.P. 01/10/2026\nVENCE: 15/01/2027"
        assertEquals(LocalDate.of(2027, 1, 15), PackageTextParser.bestExpiry(t, today))
    }

    @Test fun fechaTextual() {
        assertEquals(LocalDate.of(2026, 12, 20), PackageTextParser.bestExpiry("F.V. 20 DIC 2026", today))
        assertEquals(LocalDate.of(2027, 3, 5), PackageTextParser.bestExpiry("EXP 05MAR27", today))
    }

    @Test fun mesYAnio() {
        assertEquals(LocalDate.of(2027, 6, 30), PackageTextParser.bestExpiry("Consumir antes de: 06/2027", today))
    }

    @Test fun fechaIso() {
        assertEquals(LocalDate.of(2027, 2, 1), PackageTextParser.bestExpiry("BEST BEFORE 2027-02-01", today))
    }

    @Test fun ingredientes() {
        val t = "INGREDIENTES: leche entera, azúcar, cultivos lácticos. Información nutricional..."
        assertEquals("leche entera, azúcar, cultivos lácticos", PackageTextParser.findIngredients(t))
    }

    @Test fun codigoBarras() {
        assertTrue(PackageTextParser.isValidEan("3017620422003"))
        assertEquals("7702177017975", PackageTextParser.findBarcode("Cod 7702177017975 Reg"))
    }
}
