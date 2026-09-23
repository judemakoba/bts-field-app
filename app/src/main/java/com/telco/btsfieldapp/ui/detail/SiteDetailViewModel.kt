package com.telco.btsfieldapp.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telco.btsfieldapp.data.repository.AuditRepository
import com.telco.btsfieldapp.data.repository.AuthRepository
import com.telco.btsfieldapp.data.repository.SiteAuditData
import com.telco.btsfieldapp.data.repository.SiteRepository
import com.telco.btsfieldapp.domain.model.Site
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SiteDetailUiState(
    val site: Site? = null,
    val auditData: SiteAuditData? = null,
    val isLoading: Boolean = true,
    val isFetchingAudit: Boolean = false,
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

    val siteId: String = savedStateHandle.get<String>("siteId") ?: ""

    private val _uiState = MutableStateFlow(SiteDetailUiState())
    val uiState: StateFlow<SiteDetailUiState> = _uiState.asStateFlow()

    init {
        loadSite()
        collectEngineerName()
    }

    private fun loadSite() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val site = siteRepository.getSiteById(siteId)
            _uiState.update { it.copy(site = site, isLoading = false) }
            // Fetch audit data for this site
            fetchAuditData()
        }
    }

    private fun fetchAuditData() {
        if (siteId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingAudit = true) }
            auditRepository.fetchSiteAudit(siteId).fold(
                onSuccess = { data ->
                    _uiState.update { it.copy(auditData = data, isFetchingAudit = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isFetchingAudit = false, error = e.message) }
                }
            )
        }
    }

    private fun collectEngineerName() {
        viewModelScope.launch {
            authRepository.userName.collect { name ->
                _uiState.update { it.copy(engineerName = name) }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            siteRepository.refreshSites().onSuccess {
                fetchAuditData()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
