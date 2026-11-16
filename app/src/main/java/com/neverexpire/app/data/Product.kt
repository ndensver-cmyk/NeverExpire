package com.neverexpire.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Nutrient(val label: String, val value: String)

data class Product(
    val id: Long,
    val name: String,
    val category: String,
    val quantity: Int,
    val expiryEpochDay: Long,
    val addedEpochDay: Long = LocalDate.now().toEpochDay(),
    val barcode: String = "",
    val brand: String = "",
    val imageUrl: String = "",
    val size: String = "",
    val description: String = "",
    val ingredients: String = "",
    val allergens: String = "",
    val labels: String = "",
    val nutriScore: String = "",
    val novaGroup: Int = 0,
    val nutrients: List<Nutrient> = emptyList(),
    val packageText: String = ""
) {
    val expiryDate: LocalDate get() = LocalDate.ofEpochDay(expiryEpochDay)

    fun daysLeft(today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, expiryDate)

    fun status(today: LocalDate = LocalDate.now(), soonDays: Int = 3): ExpiryStatus {
        val d = daysLeft(today)
        return when {
            d < 0 -> ExpiryStatus.EXPIRED
            d <= soonDays -> ExpiryStatus.URGENT
            d <= 7.coerceAtLeast(soonDays + 1) -> ExpiryStatus.SOON
            else -> ExpiryStatus.OK
        }
    }

    /** Progreso de vida útil (0 = recién comprado, 1 = vencido). */
    fun lifeProgress(today: LocalDate = LocalDate.now()): Float {
        val total = (expiryEpochDay - addedEpochDay).coerceAtLeast(1)
        val used = today.toEpochDay() - addedEpochDay
        return (used.toFloat() / total).coerceIn(0f, 1f)
    }
}

enum class ExpiryStatus(val label: String) {
    EXPIRED("Vencidos"), URGENT("Urgentes"), SOON("Pronto"), OK("Al día")
}

val CATEGORIES = listOf(
    "Lácteos", "Carnes", "Frutas", "Verduras", "Panadería",
    "Enlatados", "Bebidas", "Congelados", "Granos", "Snacks", "Salsas", "Otros"
)

/** Traduce las categorías de Open Food Facts a las de la app. */
fun guessCategory(tags: List<String>, name: String = ""): String {
    val t = (tags.joinToString(" ") + " " + name).lowercase()
    return when {
        listOf("dair", "milk", "cheese", "yogurt", "leche", "queso", "yogur", "kumis", "crema").any { it in t } -> "Lácteos"
        listOf("meat", "chicken", "sausage", "carne", "pollo", "salchich", "jamon", "fish", "atun", "tuna").any { it in t } &&
            !("canned" in t) -> "Carnes"
        listOf("canned", "enlatad", "conserva").any { it in t } -> "Enlatados"
        listOf("frozen", "congelad", "ice-cream", "helado").any { it in t } -> "Congelados"
        listOf("beverage", "drink", "juice", "water", "soda", "bebida", "jugo", "gaseosa", "cafe", "coffee", "tea").any { it in t } -> "Bebidas"
        listOf("bread", "pan ", "bakery", "biscuit", "galleta", "tostada").any { it in t } -> "Panadería"
        listOf("fruit", "fruta").any { it in t } -> "Frutas"
        listOf("vegetable", "verdura", "legume").any { it in t } -> "Verduras"
        listOf("cereal", "rice", "arroz", "pasta", "grain", "lenteja", "frijol", "avena").any { it in t } -> "Granos"
        listOf("snack", "chip", "chocolate", "candy", "confection", "spread", "dulce").any { it in t } -> "Snacks"
        listOf("sauce", "salsa", "condiment", "mayonnaise", "ketchup").any { it in t } -> "Salsas"
        else -> "Otros"
    }
}
