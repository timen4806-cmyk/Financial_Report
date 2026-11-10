package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(entities = [KasTransaction::class, UserAccount::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kasDao(): KasDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_reports_v13.db"
                )
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val userDao = database.userDao()
                        val kasDao = database.kasDao()

                        // Default admin user
                        userDao.insertUser(UserAccount(username = "admin", password = "admin234"))

                        // Default initial sample data
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        kasDao.insertTransaction(
                            KasTransaction(
                                tanggal = today,
                                uraian = "Saldo Awal Kas HZdotCOM",
                                masuk = 10000000,
                                keluar = 0,
                                grup = "Laporan Keuangan"
                            )
                        )
                        kasDao.insertTransaction(
                            KasTransaction(
                                tanggal = today,
                                uraian = "Pembelian ATK & Kertas HVS",
                                masuk = 0,
                                keluar = 350000,
                                grup = "Laporan Keuangan"
                            )
                        )
                        kasDao.insertTransaction(
                            KasTransaction(
                                tanggal = today,
                                uraian = "Pembayaran Jasa Maintenance Website",
                                masuk = 2500000,
                                keluar = 0,
                                grup = "Laporan Keuangan"
                            )
                        )
                    }
                }
            }
        }
    }
}
