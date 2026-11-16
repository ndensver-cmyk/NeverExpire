package com.neverexpire.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neverexpire.app.data.Product
import com.neverexpire.app.data.ProductRepository
import com.neverexpire.app.work.ExpiryScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class InventoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ProductRepository.get(app)

    val query = MutableStateFlow("")
    val categoryFilter = MutableStateFlow<String?>(null)

    /** Ordenado por fecha de vencimiento: lo que vence primero, arriba (FEFO). */
    val products: StateFlow<List<Product>> =
        combine(repo.products, query, categoryFilter) { list, q, cat ->
            list.asSequence()
                .filter { q.isBlank() || it.name.contains(q.trim(), ignoreCase = true) }
                .filter { cat == null || it.category == cat }
                .sortedWith(compareBy<Product> { it.expiryEpochDay }.thenBy { it.name })
                .toList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val all: StateFlow<List<Product>> = repo.products

    fun add(name: String, category: String, qty: Int, expiryEpochDay: Long) =
        repo.add(name, category, qty, expiryEpochDay)

    fun update(p: Product) = repo.update(p)
    fun delete(id: Long) = repo.delete(id)
    fun consumeOne(id: Long) = repo.consumeOne(id)
    fun deleteExpired() = repo.deleteExpired()
    fun testNotification() = ExpiryScheduler.checkNow(getApplication())
}
