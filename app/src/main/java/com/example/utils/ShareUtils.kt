package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLEncoder

object ShareUtils {

    /**
     * Shares generated PDF file securely via Android FileProvider
     */
    fun sharePdfFile(context: Context, pdfFile: File) {
        try {
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan ${pdfFile.name}")
                putExtra(Intent.EXTRA_TEXT, "Berikut adalah Laporan Keuangan HZdotCOM file PDF yang telah dicetak.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Bagikan PDF Laporan Keuangan ke..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan file PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the saved PDF file using an installed PDF viewer
     */
    fun openPdfFile(context: Context, pdfFile: File, publicUri: Uri? = null) {
        try {
            val uriToOpen = publicUri ?: FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uriToOpen, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(viewIntent, "Buka file PDF dengan..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Tidak ada aplikasi pembuka PDF yang ditemukan.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares financial summary text directly via WhatsApp or System Share
     */
    fun shareSummaryText(
        context: Context,
        groupTitle: String,
        totalMasuk: Long,
        totalKeluar: Long,
        saldoAkhir: Long,
        itemCount: Int
    ) {
        val message = """
            📊 *LAPORAN KEUANGAN ${groupTitle.uppercase()}* 📊
            _Aplikasi Financial Reports HZdotCOM_
            
            🔹 Total Pemasukan  : ${PdfExporter.formatRupiah(totalMasuk)}
            🔸 Total Pengeluaran: ${PdfExporter.formatRupiah(totalKeluar)}
            ------------------------------------------
            💰 *SALDO AKHIR*    : ${PdfExporter.formatRupiah(saldoAkhir)}
            
            📝 Jumlah Transaksi: $itemCount transaksi
            
            _Dicetak & Dibagikan secara aman dari Financial Reports App._
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan $groupTitle")
        }

        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Ringkasan Keuangan ke..."))
    }

    /**
     * Direct WhatsApp message share link fallback
     */
    fun openWhatsAppMessage(context: Context, groupTitle: String, textMessage: String? = null) {
        try {
            val text = textMessage ?: "Finance_Report $groupTitle telah selesai dicetak & siap didownload."
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=$encodedText"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
}
