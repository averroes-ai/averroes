package com.rizilab.averroes.presentation.crypto

import androidx.lifecycle.viewModelScope
import com.rizilab.averroes.data.api.ApiResult
import com.rizilab.averroes.data.model.HalalCrypto
import com.rizilab.averroes.data.model.HalalStatus
import com.rizilab.averroes.data.repository.CryptoRepository
import com.rizilab.averroes.presentation.base.LoadingState
import com.rizilab.averroes.presentation.base.MviViewModel
import kotlinx.coroutines.launch

/**
 * Crypto Screen State
 */
data class CryptoState(
    val cryptos: LoadingState<List<HalalCrypto>> = LoadingState.loading(),
    val selectedFilter: HalalStatus? = null,
    val searchQuery: String = "",
    val selectedCrypto: HalalCrypto? = null,
    val isRefreshing: Boolean = false
)

/**
 * Crypto Screen Intents
 */
sealed class CryptoIntent {
    object LoadCryptos : CryptoIntent()
    object RefreshCryptos : CryptoIntent()
    data class FilterByStatus(val status: HalalStatus?) : CryptoIntent()
    data class SearchCryptos(val query: String) : CryptoIntent()
    data class SelectCrypto(val crypto: HalalCrypto) : CryptoIntent()
    object ClearSelection : CryptoIntent()
}

/**
 * Crypto Screen Effects
 */
sealed class CryptoEffect {
    data class ShowError(val message: String) : CryptoEffect()
    data class ShowCryptoDetails(val crypto: HalalCrypto) : CryptoEffect()
    object ShowRefreshSuccess : CryptoEffect()
}

/**
 * Crypto ViewModel using MVI pattern with real API data
 */
class CryptoViewModel : MviViewModel<CryptoState, CryptoIntent, CryptoEffect>(CryptoState()) {
    private val cryptoRepository = CryptoRepository()

    init {
        handleIntent(CryptoIntent.LoadCryptos)
    }

    override fun handleIntent(intent: CryptoIntent) {
        when (intent) {
            is CryptoIntent.LoadCryptos -> loadCryptos()
            is CryptoIntent.RefreshCryptos -> refreshCryptos()
            is CryptoIntent.FilterByStatus -> filterByStatus(intent.status)
            is CryptoIntent.SearchCryptos -> searchCryptos(intent.query)
            is CryptoIntent.SelectCrypto -> selectCrypto(intent.crypto)
            is CryptoIntent.ClearSelection -> clearSelection()
        }
    }

    private fun loadCryptos() {
        viewModelScope.launch {
            updateState { copy(cryptos = LoadingState.loading()) }

            when (val result = cryptoRepository.getHalalCryptocurrencies()) {
                is ApiResult.Success -> {
                    updateState {
                        copy(cryptos = LoadingState.success(result.data))
                    }
                }
                is ApiResult.Error -> {
                    updateState {
                        copy(cryptos = LoadingState.error(result.message))
                    }
                    sendEffect(CryptoEffect.ShowError("Failed to load cryptocurrencies: ${result.message}"))
                }
                is ApiResult.NetworkError -> {
                    updateState {
                        copy(cryptos = LoadingState.error("Network error. Please check your connection."))
                    }
                    sendEffect(CryptoEffect.ShowError("Network error. Please check your connection."))
                }
                is ApiResult.Loading -> {
                    // Already handled above
                }
            }
        }
    }

    private fun refreshCryptos() {
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }

            when (val result = cryptoRepository.getHalalCryptocurrencies()) {
                is ApiResult.Success -> {
                    updateState {
                        copy(
                            cryptos = LoadingState.success(result.data),
                            isRefreshing = false
                        )
                    }
                    sendEffect(CryptoEffect.ShowRefreshSuccess)
                }
                is ApiResult.Error -> {
                    updateState { copy(isRefreshing = false) }
                    sendEffect(CryptoEffect.ShowError("Failed to refresh data: ${result.message}"))
                }
                is ApiResult.NetworkError -> {
                    updateState { copy(isRefreshing = false) }
                    sendEffect(CryptoEffect.ShowError("Network error. Please check your connection."))
                }
                is ApiResult.Loading -> {
                    // Already handled above
                }
            }
        }
    }

    private fun filterByStatus(status: HalalStatus?) {
        updateState { copy(selectedFilter = status) }

        // If filtering by specific status, load only those cryptos
        if (status != null) {
            viewModelScope.launch {
                updateState { copy(cryptos = LoadingState.loading()) }

                when (val result = cryptoRepository.getCryptocurrenciesByStatus(status)) {
                    is ApiResult.Success -> {
                        updateState {
                            copy(cryptos = LoadingState.success(result.data))
                        }
                    }
                    is ApiResult.Error -> {
                        updateState {
                            copy(cryptos = LoadingState.error(result.message))
                        }
                        sendEffect(CryptoEffect.ShowError("Failed to filter cryptocurrencies: ${result.message}"))
                    }
                    is ApiResult.NetworkError -> {
                        updateState {
                            copy(cryptos = LoadingState.error("Network error. Please check your connection."))
                        }
                        sendEffect(CryptoEffect.ShowError("Network error. Please check your connection."))
                    }
                    is ApiResult.Loading -> {
                        // Already handled above
                    }
                }
            }
        } else {
            // Load all cryptos when no filter is selected
            loadCryptos()
        }
    }

    private fun searchCryptos(query: String) {
        updateState { copy(searchQuery = query) }

        if (query.isNotBlank()) {
            viewModelScope.launch {
                when (val result = cryptoRepository.searchCryptocurrencies(query)) {
                    is ApiResult.Success -> {
                        updateState {
                            copy(cryptos = LoadingState.success(result.data))
                        }
                    }
                    is ApiResult.Error -> {
                        sendEffect(CryptoEffect.ShowError("Search failed: ${result.message}"))
                    }
                    is ApiResult.NetworkError -> {
                        sendEffect(CryptoEffect.ShowError("Network error during search"))
                    }
                    is ApiResult.Loading -> {
                        // Handle if needed
                    }
                }
            }
        } else {
            // Reload all cryptos when search is cleared
            loadCryptos()
        }
    }

    private fun selectCrypto(crypto: HalalCrypto) {
        updateState { copy(selectedCrypto = crypto) }
        sendEffect(CryptoEffect.ShowCryptoDetails(crypto))
    }

    private fun clearSelection() {
        updateState { copy(selectedCrypto = null) }
    }



    // Helper function to get filtered and searched cryptos
    fun getFilteredCryptos(): List<HalalCrypto> {
        val cryptos = state.value.cryptos.data ?: return emptyList()
        
        var filtered = cryptos
        
        // Apply status filter
        state.value.selectedFilter?.let { filter ->
            filtered = filtered.filter { it.halalStatus == filter }
        }
        
        // Apply search filter
        if (state.value.searchQuery.isNotBlank()) {
            val query = state.value.searchQuery.lowercase()
            filtered = filtered.filter { crypto ->
                crypto.name.lowercase().contains(query) ||
                crypto.symbol.lowercase().contains(query)
            }
        }
        
        return filtered.sortedBy { it.rank }
    }
}
