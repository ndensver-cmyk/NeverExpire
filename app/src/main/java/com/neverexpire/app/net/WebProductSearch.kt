package com.neverexpire.app.net

import com.neverexpire.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Resultado público de una búsqueda web; siempre conserva el enlace de origen. */
data class WebSearchResult(
    val title: String,
    val url: String,
    val description: String
)

/**
 * Búsqueda web de respaldo. La clave se inyecta desde GitHub Actions Secrets;
 * nunca se escribe en el repositorio ni se incluye como literal en el código.
 */
object WebProductSearch {
    suspend fun search(query: String): List<WebSearchResult> = withContext(Dispatchers.IO) {
        val key = BuildConfig.BRAVE_SEARCH_API_KEY
        if (key.isBlank() || query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val connection = try {
            URL("https://api.search.brave.com/res/v1/web/search?q=$encoded&country=CO&search_lang=es&count=8")
                .openConnection() as HttpURLConnection
        } catch (_: Exception) { return@withContext emptyList() }
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Subscription-Token", key)
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            if (connection.responseCode !in 200..299) return@withContext emptyList()
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val results = json.optJSONObject("web")?.optJSONArray("results")
                ?: return@withContext emptyList()
            (0 until results.length()).mapNotNull { index ->
                val item = results.optJSONObject(index) ?: return@mapNotNull null
                val title = item.optString("title").trim()
                val url = item.optString("url").trim()
                val description = item.optString("description").trim()
                if (title.isBlank() || !url.startsWith("https://")) null
                else WebSearchResult(title, url, description)
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}
