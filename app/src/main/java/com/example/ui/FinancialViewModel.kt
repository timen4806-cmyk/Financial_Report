package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FinancialRepository
import com.example.data.KasTransaction
import com.example.utils.PdfExportResult
import com.example.utils.PdfExporter
import com.example.utils.PdfSaveLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FinancialSummary(
    val totalMasuk: Long = 0,
    val totalKeluar: Long = 0,
    val saldoAkhir: Long = 0
)

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinancialRepository = FinancialRepository(
        AppDatabase.getDatabase(application),
        application
    )

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _activeGroup = MutableStateFlow(repository.getActiveGroup())
    val activeGroup: StateFlow<String> = _activeGroup.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTransactionIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTransactionIds: StateFlow<Set<Long>> = _selectedTransactionIds.asStateFlow()

    private val _darkModeSetting = MutableStateFlow(repository.getDarkModeSetting())
    val darkModeSetting: StateFlow<Int> = _darkModeSetting.asStateFlow()

    private val _logoUri = MutableStateFlow(repository.getLogoUri())
    val logoUri: StateFlow<String?> = _logoUri.asStateFlow()

    val allGroups: StateFlow<List<String>> = repository.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Laporan Keuangan"))

    // Reactive list of transactions filtered by active group and search query
    val transactions: StateFlow<List<KasTransaction>> = combine(
        _activeGroup,
        _searchQuery
    ) { group, query ->
        group to query
    }.combine(repository.getAllGroups()) { (group, query), _ ->
        group to query
    }.combine(
        repository.getTransactionsForGroup(_activeGroup.value)
    ) { (group, query), rawList ->
        if (query.isBlank()) {
            rawList
        } else {
            val q = query.lowercase(Locale.getDefault())
            rawList.filter {
                it.uraian.lowercase(Locale.getDefault()).contains(q) ||
                it.tanggal.contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Summary calculation
    val summary: StateFlow<FinancialSummary> = transactions.combine(_activeGroup) { list, _ ->
        var totalMasuk = 0L
        var totalKeluar = 0L
        list.forEach {
            totalMasuk += it.masuk
            totalKeluar += it.keluar
        }
        FinancialSummary(
            totalMasuk = totalMasuk,
            totalKeluar = totalKeluar,
            saldoAkhir = totalMasuk - totalKeluar
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

    fun login(u: String, p: String) {
        viewModelScope.launch {
            val user = repository.getUserByUsername(u.trim())
            if (user != null && user.password == p) {
                _isLoggedIn.value = true
                _loginError.value = null
            } else {
                _loginError.value = "Username atau Password salah!"
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _selectedTransactionIds.value = emptySet()
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun updateCredentials(oldUser: String, newUser: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val current = repository.getUserByUsername(oldUser.trim())
            if (current != null) {
                repository.updateUserCredentials(oldUser.trim(), newUser.trim(), newPass, )
                onResult(true, "Username & Password berhasil diperbarui!")
            } else {
                onResult(false, "User lama tidak valid!")
            }
        }
    }

    fun selectGroup(groupName: String) {
        val upper = groupName.trim().uppercase(Locale.getDefault())
        _activeGroup.value = if (upper.isBlank()) "LAPORAN KEUANGAN" else upper
        repository.setActiveGroup(_activeGroup.value)
        _selectedTransactionIds.value = emptySet()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(id: Long) {
        val current = _selectedTransactionIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedTransactionIds.value = current
    }

    fun clearSelections() {
        _selectedTransactionIds.value = emptySet()
    }

    fun selectAll(select: Boolean) {
        if (select) {
            _selectedTransactionIds.value = transactions.value.map { it.id }.toSet()
        } else {
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun addTransaction(uraian: String, masukStr: String, keluarStr: String, dateStr: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val masuk = masukStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
            val keluar = keluarStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
            val tgl = dateStr.ifBlank {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }
            repository.insertTransaction(
                KasTransaction(
                    tanggal = tgl,
                    uraian = uraian.trim(),
                    masuk = masuk,
                    keluar = keluar,
                    grup = _activeGroup.value
                )
            )
            onComplete()
        }
    }

    fun updateTransaction(id: Long, uraian: String, masukStr: String, keluarStr: String, dateStr: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val masuk = masukStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
            val keluar = keluarStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
            val tgl = dateStr.ifBlank {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }
            repository.updateTransaction(
                KasTransaction(
                    id = id,
                    tanggal = tgl,
                    uraian = uraian.trim(),
                    masuk = masuk,
                    keluar = keluar,
                    grup = _activeGroup.value
                )
            )
            onComplete()
        }
    }

    fun deleteSelectedTransactions() {
        viewModelScope.launch {
            val ids = _selectedTransactionIds.value.toList()
            if (ids.isNotEmpty()) {
                repository.deleteTransactionsByIds(ids)
                _selectedTransactionIds.value = emptySet()
            }
        }
    }

    fun setDarkModeSetting(setting: Int) {
        _darkModeSetting.value = setting
        repository.setDarkModeSetting(setting)
    }

    fun setLogoUri(uri: Uri?) {
        val uriStr = uri?.toString()
        _logoUri.value = uriStr
        repository.setLogoUri(uriStr)
    }

    fun generatePdfReport(
        customTitle: String,
        periodeManual: String,
        saveLocation: PdfSaveLocation = PdfSaveLocation.DOWNLOADS
    ): PdfExportResult? {
        val context = getApplication<Application>().applicationContext
        val reportTitle = customTitle.trim().ifBlank { _activeGroup.value }
        return PdfExporter.generatePdf(
            context = context,
            groupTitle = reportTitle,
            periodeText = periodeManual.ifBlank { SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date()) },
            logoUriString = _logoUri.value,
            transactions = transactions.value,
            saveLocation = saveLocation
        )
    }
}
