package com.neverexpire.app.net

import com.neverexpire.app.data.Nutrient
import com.neverexpire.app.data.guessCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Información de un producto obtenida de Open Food Facts (base de datos abierta y gratuita). */
data class ProductInfo(
    val barcode: String,
    val name: String,
    val brand: String,
    val imageUrl: String,
    val size: String,
    val category: String,
    val description: String,
    val ingredients: String,
    val allergens: String,
    val labels: String,
    val nutriScore: String,
    val novaGroup: Int,
    val nutrients: List<Nutrient>
)

object OpenFoodFacts {
    private const val BASE = "https://world.openfoodfacts.org"
    private const val UA = "NeverExpire/2.0 (Android; github.com/ndensver-cmyk/NeverExpire)"
    private const val FIELDS = "code,product_name,product_name_es,generic_name,generic_name_es,brands,quantity," +
        "image_front_url,image_front_small_url,image_small_url,image_url,categories_tags,categories,ingredients_text,ingredients_text_es," +
        "allergens_tags,nutriscore_grade,nova_group,nutriments,labels,origins,countries"

    suspend fun byBarcode(code: String): ProductInfo? = withContext(Dispatchers.IO) {
        val json = get("$BASE/api/v2/product/${code.filter { it.isDigit() }}.json?lc=es&fields=$FIELDS")
            ?: return@withContext null
        if (json.optInt("status", 0) != 1) return@withContext null
        json.optJSONObject("product")?.let { parse(it, code) }
    }

    suspend fun search(query: String): List<ProductInfo> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val json = get(
            "$BASE/cgi/search.pl?search_terms=$q&search_simple=1&action=process&json=1" +
                "&page_size=12&lc=es&sort_by=unique_scans_n&fields=$FIELDS"
        ) ?: return@withContext emptyList()
        val arr = json.optJSONArray("products") ?: JSONArray()
        (0 until arr.length()).mapNotNull { parse(arr.getJSONObject(it), null) }
            .filter { it.name.isNotBlank() }
    }

    private fun get(url: String): JSONObject? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", UA)
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        try {
            if (c.responseCode !in 200..299) null
            else JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        } finally { c.disconnect() }
    } catch (e: Exception) { null }

    private fun JSONObject.s(vararg keys: String): String {
        for (k in keys) { val v = optString(k, "").trim(); if (v.isNotEmpty() && v != "null") return v }
        return ""
    }

    private fun parse(p: JSONObject, fallbackCode: String?): ProductInfo {
        val brand = p.s("brands").split(",").firstOrNull()?.trim().orEmpty()
        val generic = p.s("generic_name_es", "generic_name")
        val name = p.s("product_name_es", "product_name").ifBlank { generic }.ifBlank { brand }
        val tags = (p.optJSONArray("categories_tags")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()) +
            p.s("categories").split(",").map { it.trim() }.filter { it.isNotBlank() }
        val allergens = p.optJSONArray("allergens_tags")?.let { a ->
            (0 until a.length()).map { translateAllergen(a.getString(it)) }
        }?.joinToString(", ").orEmpty()
        val nutriScore = p.s("nutriscore_grade").uppercase().takeIf { it.length == 1 }.orEmpty()
        val nova = p.optInt("nova_group", 0)
        val nutrients = parseNutrients(p.optJSONObject("nutriments"))
        val ingredients = p.s("ingredients_text_es", "ingredients_text")
        val labels = p.s("labels")
        val size = p.s("quantity")

        val description = buildString {
            append(generic.ifBlank { name })
            if (brand.isNotBlank()) append(" de la marca $brand")
            if (size.isNotBlank()) append(", presentación de $size")
            append(".")
            val cat = p.s("categories").split(",").take(2).joinToString(", ") { it.trim() }
            if (cat.isNotBlank()) append(" Categoría: $cat.")
            if (nutriScore.isNotBlank()) append(" Nutri-Score $nutriScore (${nutriScoreText(nutriScore)}).")
            if (nova in 1..4) append(" NOVA $nova: ${novaText(nova)}.")
            nutrients.firstOrNull { it.label.startsWith("Energía") }?.let { append(" Aporta ${it.value} por 100 g.") }
            if (allergens.isNotBlank()) append(" Contiene: $allergens.")
            val origins = p.s("origins")
            if (origins.isNotBlank()) append(" Origen: $origins.")
        }

        return ProductInfo(
            barcode = p.s("code").ifBlank { fallbackCode.orEmpty() },
            name = name,
            brand = brand,
            imageUrl = p.s("image_front_url", "image_front_small_url", "image_small_url", "image_url"),
            size = size,
            category = guessCategory(tags, "$name $brand $generic $ingredients $labels"),
            description = description,
            ingredients = ingredients,
            allergens = allergens,
            labels = labels,
            nutriScore = nutriScore,
            novaGroup = nova,
            nutrients = nutrients
        )
    }

    private fun parseNutrients(n: JSONObject?): List<Nutrient> {
        if (n == null) return emptyList()
        val map = listOf(
            "energy-kcal_100g" to "Energía|kcal",
            "fat_100g" to "Grasas|g",
            "saturated-fat_100g" to "Grasas saturadas|g",
            "carbohydrates_100g" to "Carbohidratos|g",
            "sugars_100g" to "Azúcares|g",
            "fiber_100g" to "Fibra|g",
            "proteins_100g" to "Proteínas|g",
            "salt_100g" to "Sal|g",
            "sodium_100g" to "Sodio|mg"
        )
        return map.mapNotNull { (key, lu) ->
            if (!n.has(key)) return@mapNotNull null
            val (label, unit) = lu.split("|")
            var v = n.optDouble(key, Double.NaN)
            if (v.isNaN()) return@mapNotNull null
            if (unit == "mg") v *= 1000
            val txt = if (v % 1.0 == 0.0) v.toLong().toString() else "%.1f".format(v)
            Nutrient(label, "$txt $unit")
        }
    }

    private fun translateAllergen(tag: String): String = when (tag.substringAfter(":")) {
        "milk" -> "leche"; "gluten" -> "gluten"; "eggs" -> "huevo"; "nuts" -> "frutos secos"
        "peanuts" -> "maní"; "soybeans" -> "soya"; "fish" -> "pescado"; "crustaceans" -> "crustáceos"
        "molluscs" -> "moluscos"; "celery" -> "apio"; "mustard" -> "mostaza"
        "sesame-seeds" -> "ajonjolí"; "sulphur-dioxide-and-sulphites" -> "sulfitos"; "lupin" -> "altramuz"
        else -> tag.substringAfter(":").replace("-", " ")
    }

    fun nutriScoreText(g: String) = when (g) {
        "A" -> "muy buena calidad nutricional"; "B" -> "buena calidad nutricional"
        "C" -> "calidad nutricional media"; "D" -> "baja calidad nutricional"
        "E" -> "muy baja calidad nutricional"; else -> ""
    }

    fun novaText(n: Int) = when (n) {
        1 -> "alimento sin procesar o mínimamente procesado"; 2 -> "ingrediente culinario procesado"
        3 -> "alimento procesado"; 4 -> "producto ultraprocesado"; else -> ""
    }
}
