package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

class FinancialRepository(private val database: AppDatabase, context: Context) {
    private val kasDao = database.kasDao()
    private val userDao = database.userDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("hz_finance_prefs", Context.MODE_PRIVATE)

    fun getTransactionsForGroup(grup: String): Flow<List<KasTransaction>> =
        kasDao.getTransactionsForGroup(grup)

    fun getAllGroups(): Flow<List<String>> = kasDao.getAllGroups()

    suspend fun insertTransaction(transaction: KasTransaction): Long =
        kasDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: KasTransaction) =
        kasDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: KasTransaction) =
        kasDao.deleteTransaction(transaction)

    suspend fun deleteTransactionsByIds(ids: List<Long>) =
        kasDao.deleteTransactionsByIds(ids)

    suspend fun getUserByUsername(username: String): UserAccount? =
        userDao.getUserByUsername(username)

    suspend fun updateUserCredentials(oldUser: String, newUser: String, newPass: String) {
        userDao.deleteAllUsers()
        userDao.insertUser(UserAccount(username = newUser, password = newPass))
    }

    // SharedPreferences for Dark Mode & Logo URI
    fun getDarkModeSetting(): Int = prefs.getInt("dark_mode_setting", 0) // 0: System, 1: Light, 2: Dark
    fun setDarkModeSetting(setting: Int) {
        prefs.edit().putInt("dark_mode_setting", setting).apply()
    }

    fun getLogoUri(): String? = prefs.getString("logo_uri", null)
    fun setLogoUri(uri: String?) {
        prefs.edit().putString("logo_uri", uri).apply()
    }

    fun getActiveGroup(): String = prefs.getString("active_group", "Laporan Keuangan") ?: "Laporan Keuangan"
    fun setActiveGroup(groupName: String) {
        prefs.edit().putString("active_group", groupName).apply()
    }
}
