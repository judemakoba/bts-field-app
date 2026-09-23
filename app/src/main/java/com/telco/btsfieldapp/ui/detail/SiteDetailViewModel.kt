package com.telco.btsfieldapp.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telco.btsfieldapp.data.repository.AuditRepository
import com.telco.btsfieldapp.data.repository.AuthRepository
import com.telco.btsfieldapp.data.repository.SiteRepository
import com.telco.btsfieldapp.domain.model.AuditRecord
import com.telco.btsfieldapp.domain.model.Site
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SiteDetailUiState(
    val site: Site? = null,
    val audits: List<AuditRecord> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTab: Int = 0,
    val engineerName: String? = null,
    val error: String? = null
)

@HiltViewModel
class SiteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val siteRepository: SiteRepository,
    private val auditRepository: AuditRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val siteId: Long = savedStateHandle.get<Long>("siteId") ?: 0L

    private val _selectedTab = MutableStateFlow(0)
    private val _site = MutableStateFlow<Site?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SiteDetailUiState> = combine(
        _site,
        _selectedTab,
        _isLoading,
        _error,
        authRepository.userName
    ) { site, tab, loading, error, engineerName ->
        SiteDetailUiState(
            site = site,
            selectedTab = tab,
            isLoading = loading,
            error = error,
            engineerName = engineerName
        )
    }.combine(
        auditRepository.getAuditsBySite(siteId)
    ) { state, audits ->
        state.copy(audits = audits)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SiteDetailUiState()
    )

    init {
        loadSite()
    }

    private fun loadSite() {
        viewModelScope.launch {
            _isLoading.value = true
            val site = siteRepository.getSiteById(siteId)
            _site.value = site
            _isLoading.value = false
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun clearError() {
        _error.value = null
    }
}
