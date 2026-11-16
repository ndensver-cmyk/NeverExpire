package com.neverexpire.app.net

import java.net.URLEncoder

/** Enlace de búsqueda web gratuita. No requiere clave ni servicio de pago. */
data class WebSearchResult(
    val title: String,
    val url: String,
    val description: String
)

/**
 * Respaldo gratuito: prepara una búsqueda pública en Google.
 * La aplicación no extrae ni inventa datos de los resultados; el usuario
 * revisa las fuentes y confirma los datos del producto y su vencimiento.
 */
object WebProductSearch {
    fun search(query: String): List<WebSearchResult> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()
        val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
        return listOf(
            WebSearchResult(
                title = "Buscar \"$cleanQuery\" en Google",
                url = "https://www.google.com/search?q=$encoded",
                description = "Abre resultados públicos de Internet. Comprueba que la marca y la presentación coincidan con tu producto."
            )
        )
    }
}
