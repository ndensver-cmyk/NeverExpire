package com.neverexpire.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Guarda el inventario en SharedPreferences como JSON.
 * Sin dependencias externas: simple y fiable.
 */
class ProductRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("neverexpire_inventory", Context.MODE_PRIVATE)

    private val _products = MutableStateFlow(load())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    fun add(name: String, category: String, quantity: Int, expiryEpochDay: Long) {
        val p = Product(
            id = System.currentTimeMillis(),
            name = name.trim(),
            category = category,
            quantity = quantity.coerceAtLeast(1),
            expiryEpochDay = expiryEpochDay
        )
        save(_products.value + p)
    }

    fun update(product: Product) {
        save(_products.value.map { if (it.id == product.id) product else it })
    }

    fun delete(id: Long) {
        save(_products.value.filterNot { it.id == id })
    }

    /** Resta 1 unidad; si llega a 0, se elimina. */
    fun consumeOne(id: Long) {
        val list = _products.value.mapNotNull {
            if (it.id != id) it
            else if (it.quantity > 1) it.copy(quantity = it.quantity - 1)
            else null
        }
        save(list)
    }

    fun deleteExpired() {
        save(_products.value.filter { it.daysLeft() >= 0 })
    }

    fun reload() {
        _products.value = load()
    }

    private fun save(list: List<Product>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("category", it.category)
                put("quantity", it.quantity)
                put("expiry", it.expiryEpochDay)
                put("added", it.addedEpochDay)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
        _products.value = list
    }

    private fun load(): List<Product> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Product(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    category = o.optString("category", "Otros"),
                    quantity = o.optInt("quantity", 1),
                    expiryEpochDay = o.getLong("expiry"),
                    addedEpochDay = o.optLong("added", o.getLong("expiry"))
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY = "products"

        @Volatile
        private var INSTANCE: ProductRepository? = null

        fun get(context: Context): ProductRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProductRepository(context).also { INSTANCE = it }
            }
    }
}
