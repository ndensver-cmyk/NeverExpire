package com.neverexpire.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Product(
    val id: Long,
    val name: String,
    val category: String,
    val quantity: Int,
    val expiryEpochDay: Long,
    val addedEpochDay: Long = LocalDate.now().toEpochDay()
) {
    val expiryDate: LocalDate get() = LocalDate.ofEpochDay(expiryEpochDay)

    fun daysLeft(today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, expiryDate)

    fun status(today: LocalDate = LocalDate.now()): ExpiryStatus {
        val d = daysLeft(today)
        return when {
            d < 0 -> ExpiryStatus.EXPIRED
            d <= 3 -> ExpiryStatus.URGENT
            d <= 7 -> ExpiryStatus.SOON
            else -> ExpiryStatus.OK
        }
    }
}

enum class ExpiryStatus { EXPIRED, URGENT, SOON, OK }

val CATEGORIES = listOf(
    "Lácteos", "Carnes", "Frutas", "Verduras", "Panadería",
    "Enlatados", "Bebidas", "Congelados", "Granos", "Otros"
)
