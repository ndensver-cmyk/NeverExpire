package com.neverexpire.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Inventario guardado localmente como JSON en SharedPreferences. */
class ProductRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("neverexpire_inventory", Context.MODE_PRIVATE)

    private val _products = MutableStateFlow(load())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    fun find(id: Long): Product? = _products.value.firstOrNull { it.id == id }

    fun upsert(p: Product): Product {
        val item = if (p.id == 0L) p.copy(id = System.currentTimeMillis()) else p
        val exists = _products.value.any { it.id == item.id }
        save(if (exists) _products.value.map { if (it.id == item.id) item else it } else _products.value + item)
        return item
    }

    fun delete(id: Long) = save(_products.value.filterNot { it.id == id })

    fun changeQuantity(id: Long, delta: Int) {
        save(_products.value.mapNotNull {
            if (it.id != id) it
            else (it.quantity + delta).let { q -> if (q <= 0) null else it.copy(quantity = q) }
        })
    }

    fun deleteExpired() = save(_products.value.filter { it.daysLeft() >= 0 })

    fun reload() { _products.value = load() }

    private fun save(list: List<Product>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY, arr.toString()).apply()
        _products.value = list
    }

    private fun toJson(p: Product) = JSONObject().apply {
        put("id", p.id); put("name", p.name); put("category", p.category)
        put("quantity", p.quantity); put("expiry", p.expiryEpochDay); put("added", p.addedEpochDay)
        put("barcode", p.barcode); put("brand", p.brand); put("imageUrl", p.imageUrl)
        put("size", p.size); put("description", p.description); put("ingredients", p.ingredients)
        put("allergens", p.allergens); put("labels", p.labels); put("nutriScore", p.nutriScore)
        put("nova", p.novaGroup); put("packageText", p.packageText)
        put("nutrients", JSONArray().apply {
            p.nutrients.forEach { put(JSONObject().put("l", it.label).put("v", it.value)) }
        })
    }

    private fun fromJson(o: JSONObject): Product {
        val nArr = o.optJSONArray("nutrients") ?: JSONArray()
        return Product(
            id = o.getLong("id"),
            name = o.getString("name"),
            category = o.optString("category", "Otros"),
            quantity = o.optInt("quantity", 1),
            expiryEpochDay = o.getLong("expiry"),
            addedEpochDay = o.optLong("added", o.getLong("expiry") - 7),
            barcode = o.optString("barcode"),
            brand = o.optString("brand"),
            imageUrl = o.optString("imageUrl"),
            size = o.optString("size"),
            description = o.optString("description"),
            ingredients = o.optString("ingredients"),
            allergens = o.optString("allergens"),
            labels = o.optString("labels"),
            nutriScore = o.optString("nutriScore"),
            novaGroup = o.optInt("nova", 0),
            packageText = o.optString("packageText"),
            nutrients = (0 until nArr.length()).map {
                val n = nArr.getJSONObject(it); Nutrient(n.getString("l"), n.getString("v"))
            }
        )
    }

    private fun load(): List<Product> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    companion object {
        private const val KEY = "products"
        @Volatile private var INSTANCE: ProductRepository? = null
        fun get(context: Context): ProductRepository =
            INSTANCE ?: synchronized(this) { INSTANCE ?: ProductRepository(context).also { INSTANCE = it } }
    }
}
