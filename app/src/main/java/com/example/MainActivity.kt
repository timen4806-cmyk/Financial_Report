package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.KasTransaction
import com.example.ui.theme.FinancialReportsTheme
import com.example.ui.FinancialViewModel
import com.example.ui.InputTransactionScreen
import com.example.ui.LoginScreen
import com.example.ui.MainScreen
import com.example.ui.PdfPreviewDialog

enum class ScreenState {
    LOGIN,
    MAIN,
    INPUT_OR_EDIT
}

class MainActivity : ComponentActivity() {

    private val viewModel: FinancialViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkModeSetting by viewModel.darkModeSetting.collectAsState()

            FinancialReportsTheme(darkModeSetting = darkModeSetting) {
                FinancialAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FinancialAppContent(viewModel: FinancialViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val activeGroup by viewModel.activeGroup.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedIds by viewModel.selectedTransactionIds.collectAsState()
    val darkModeSetting by viewModel.darkModeSetting.collectAsState()
    val logoUri by viewModel.logoUri.collectAsState()
    val loginError by viewModel.loginError.collectAsState()

    var currentScreen by remember { mutableStateOf(ScreenState.LOGIN) }
    var editingTransaction by remember { mutableStateOf<KasTransaction?>(null) }
    var showPdfDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSubmit = { u, p ->
                viewModel.login(u, p)
            },
            errorMessage = loginError
        )
    } else {
        when (currentScreen) {
            ScreenState.LOGIN -> {
                // If logged in, navigate to MAIN
                currentScreen = ScreenState.MAIN
            }
            ScreenState.MAIN -> {
                MainScreen(
                    activeGroup = activeGroup,
                    allGroups = allGroups,
                    transactions = transactions,
                    summary = summary,
                    searchQuery = searchQuery,
                    selectedIds = selectedIds,
                    darkModeSetting = darkModeSetting,
                    logoUri = logoUri,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onSelectGroup = { group ->
                        viewModel.selectGroup(group)
                    },
                    onToggleSelection = { id -> viewModel.toggleSelection(id) },
                    onSelectAll = { select -> viewModel.selectAll(select) },
                    onDeleteSelected = {
                        viewModel.deleteSelectedTransactions()
                        Toast.makeText(context, "Data berhasil dihapus!", Toast.LENGTH_SHORT).show()
                    },
                    onOpenAddScreen = {
                        editingTransaction = null
                        currentScreen = ScreenState.INPUT_OR_EDIT
                    },
                    onOpenEditScreen = { item ->
                        editingTransaction = item
                        currentScreen = ScreenState.INPUT_OR_EDIT
                    },
                    onSetDarkMode = { setting -> viewModel.setDarkModeSetting(setting) },
                    onSetLogoUri = { uri ->
                        viewModel.setLogoUri(uri)
                        if (uri != null) {
                            Toast.makeText(context, "Logo dipilih!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onUpdateAccount = { oldU, newU, newP, callback ->
                        viewModel.updateCredentials(oldU, newU, newP, callback)
                    },
                    onLogout = {
                        viewModel.logout()
                        currentScreen = ScreenState.LOGIN
                    },
                    onExportPdfClick = {
                        showPdfDialog = true
                    }
                )
            }
            ScreenState.INPUT_OR_EDIT -> {
                InputTransactionScreen(
                    editingTransaction = editingTransaction,
                    onSave = { uraian, masuk, keluar, date ->
                        viewModel.addTransaction(uraian, masuk, keluar, date) {
                            Toast.makeText(context, "Data Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                            currentScreen = ScreenState.MAIN
                        }
                    },
                    onUpdate = { id, uraian, masuk, keluar, date ->
                        viewModel.updateTransaction(id, uraian, masuk, keluar, date) {
                            Toast.makeText(context, "Data Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                            currentScreen = ScreenState.MAIN
                        }
                    },
                    onBack = {
                        currentScreen = ScreenState.MAIN
                    }
                )
            }
        }

        if (showPdfDialog) {
            PdfPreviewDialog(
                activeGroup = activeGroup,
                totalMasuk = summary.totalMasuk,
                totalKeluar = summary.totalKeluar,
                saldoAkhir = summary.saldoAkhir,
                itemCount = transactions.size,
                onGeneratePdf = { periode ->
                    viewModel.generatePdfReport(periode)
                },
                onDismiss = { showPdfDialog = false }
            )
        }
    }
}
