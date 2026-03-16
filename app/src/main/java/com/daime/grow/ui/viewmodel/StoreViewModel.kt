package com.daime.grow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daime.grow.data.remote.SupabaseClient
import com.daime.grow.data.remote.model.ProductDto
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StoreUiState(
    val products: List<ProductDto> = emptyList(),
    val banners: List<com.daime.grow.data.remote.model.BannerDto> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class StoreViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val supabase = SupabaseClient.clientOrNull

    init {
        android.util.Log.d("StoreViewModel", "Iniciando StoreViewModel")
        android.util.Log.d("StoreViewModel", "URL configurada: ${com.daime.grow.BuildConfig.SUPABASE_URL.take(15)}...")
        android.util.Log.d("StoreViewModel", "Client instanciado: ${supabase != null}")
        
        loadCategories()
        loadProducts()
        loadBanners()
    }

    private fun loadBanners() {
        viewModelScope.launch {
            try {
                val client = supabase
                if (client == null) {
                    android.util.Log.w("StoreViewModel", "Supabase não configurado, banners não carregados")
                    return@launch
                }
                val bannersList = client.from("banners")
                    .select()
                    .decodeList<com.daime.grow.data.remote.model.BannerDto>()
                    .filter { it.isActive != false }
                
                android.util.Log.d("StoreViewModel", "Banners carregados: ${bannersList.size}")
                _uiState.value = _uiState.value.copy(banners = bannersList)
            } catch (e: Exception) {
                android.util.Log.e("StoreViewModel", "Erro fatal ao carregar banners: ${e.message}", e)
                if (e is kotlinx.serialization.SerializationException) {
                    android.util.Log.e("StoreViewModel", "Erro de serialização nos banners - verifique se os campos do DTO batem com o banco")
                }
            }
        }
    }

    fun loadProducts(category: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val client = supabase
                if (client == null) {
                    android.util.Log.e("StoreViewModel", "Supabase não configurado")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Supabase não configurado"
                    )
                    return@launch
                }

                android.util.Log.d("StoreViewModel", "Carregando produtos...")
                val products = client.from("products")
                    .select()
                    .decodeList<ProductDto>()

                android.util.Log.d("StoreViewModel", "Produtos recebidos: ${products.size}")

                // Filtrar produtos
                val filtered = products
                    .filter { it.isActive }
                    .let { list ->
                        if (!category.isNullOrBlank()) {
                            list.filter { it.category == category }
                        } else {
                            list
                        }
                    }
                    .sortedByDescending { it.createdAt }

                android.util.Log.d("StoreViewModel", "Produtos filtrados (ativos): ${filtered.size}")

                _uiState.value = _uiState.value.copy(
                    products = filtered,
                    isLoading = false
                )
            } catch (e: Exception) {
                android.util.Log.e("StoreViewModel", "Erro fatal ao carregar produtos: ${e.message}", e)
                if (e is kotlinx.serialization.SerializationException) {
                    android.util.Log.e("StoreViewModel", "Erro de serialização nos produtos - verifique se os campos do DTO batem com o banco")
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar produtos: ${e.message}"
                )
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val supabase = supabase
                if (supabase == null) return@launch

                // Buscar todos produtos ativos e extrair categorias
                val products = supabase.from("products")
                    .select()
                    .decodeList<ProductDto>()

                val categories = products
                    .filter { it.isActive }
                    .mapNotNull { it.category }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                _uiState.value = _uiState.value.copy(
                    categories = categories
                )
            } catch (e: Exception) {
                // Usar categorias padrão se falhar
                _uiState.value = _uiState.value.copy(
                    categories = listOf(
                        "Sementes",
                        "Substratos",
                        "Nutrientes",
                        "Acessórios",
                        "Equipamentos"
                    )
                )
            }
        }
    }

    fun setSelectedCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadProducts(category)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            selectedCategory = null,
            searchQuery = ""
        )
        loadProducts(null)
    }
}
