package com.telco.btsfieldapp.ui.sites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telco.btsfieldapp.data.repository.AuthRepository
import com.telco.btsfieldapp.data.repository.SiteRepository
import com.telco.btsfieldapp.domain.model.Site
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SitesUiState(
    val sites: List<Site> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null,
    val userName: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SitesViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SitesUiState> = combine(
        _searchQuery,
        _isRefreshing,
        _error,
        authRepository.userName
    ) { query, refreshing, error, userName ->
        SitesUiState(
            searchQuery = query,
            isRefreshing = refreshing,
            error = error,
            userName = userName
        )
    }.combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) siteRepository.getAllSites()
            else siteRepository.searchSites(query)
        }
    ) { state, sites ->
        state.copy(sites = sites, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SitesUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            siteRepository.refreshSites().fold(
                onSuccess = { _error.value = null },
                onFailure = { _error.value = it.message }
            )
            _isRefreshing.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
