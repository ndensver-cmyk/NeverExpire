package com.neverexpire.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neverexpire.app.data.AppSettings
import com.neverexpire.app.data.ExpiryStatus
import com.neverexpire.app.data.Product
import com.neverexpire.app.data.ProductRepository
import com.neverexpire.app.data.SettingsRepository
import com.neverexpire.app.net.OpenFoodFacts
import com.neverexpire.app.net.ProductInfo
import com.neverexpire.app.net.WebProductSearch
import com.neverexpire.app.net.WebSearchResult
import com.neverexpire.app.scan.ScanDraft
import com.neverexpire.app.work.ExpiryScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class InventoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ProductRepository.get(app)
    private val settingsRepo = SettingsRepository.get(app)

    val settings: StateFlow<AppSettings> = settingsRepo.settings
    val all: StateFlow<List<Product>> = repo.products

    val query = MutableStateFlow("")
    val categoryFilter = MutableStateFlow<String?>(null)
    val statusFilter = MutableStateFlow<ExpiryStatus?>(null)

    /** Borrador que llega desde el escáner. */
    val scanDraft = MutableStateFlow<ScanDraft?>(null)

    /** Lista ordenada por vencimiento: lo primero que vence, arriba (FEFO). */
    val products: StateFlow<List<Product>> =
        combine(repo.products, query, categoryFilter, statusFilter, settings) { list, q, cat, st, s ->
            list.asSequence()
                .filter { q.isBlank() || it.name.contains(q.trim(), true) || it.brand.contains(q.trim(), true) }
                .filter { cat == null || it.category == cat }
                .filter { st == null || it.status(soonDays = s.alertDays) == st }
                .sortedWith(compareBy<Product> { it.expiryEpochDay }.thenBy { it.name })
                .toList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun find(id: Long) = repo.find(id)
    fun save(p: Product) = repo.upsert(p)
    fun delete(id: Long) = repo.delete(id)
    fun restore(p: Product) { repo.upsert(p) }
    fun changeQty(id: Long, delta: Int) = repo.changeQuantity(id, delta)
    fun deleteExpired() = repo.deleteExpired()
    fun testNotification() = ExpiryScheduler.checkNow(getApplication())

    fun updateSettings(s: AppSettings) {
        val hourChanged = s.notifyHour != settings.value.notifyHour
        settingsRepo.update(s)
        if (hourChanged) ExpiryScheduler.schedule(getApplication(), replace = true)
    }

    suspend fun lookupBarcode(code: String): ProductInfo? = OpenFoodFacts.byBarcode(code)
    suspend fun searchByName(q: String): List<ProductInfo> = OpenFoodFacts.search(q)
    suspend fun searchWeb(q: String): List<WebSearchResult> = WebProductSearch.search(q)
}
