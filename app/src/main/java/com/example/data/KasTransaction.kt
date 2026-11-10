package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kas")
data class KasTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tanggal: String, // YYYY-MM-DD
    val uraian: String,
    val masuk: Long = 0,
    val keluar: Long = 0,
    val grup: String = "Laporan Keuangan"
)
