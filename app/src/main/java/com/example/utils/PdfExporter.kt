package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.KasTransaction
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PdfSaveLocation(val label: String, val folderDisplay: String) {
    DOWNLOADS("Download", "Download/FinancialReports"),
    DOCUMENTS("Documents", "Documents/FinancialReports")
}

data class PdfExportResult(
    val cacheFile: File,
    val publicUri: Uri?,
    val displayPath: String,
    val fileName: String,
    val savedToPublicStorage: Boolean
)

object PdfExporter {

    private const val SUBFOLDER_NAME = "FinancialReports"

    fun formatRupiah(number: Long): String {
        val formatter = NumberFormat.getInstance(Locale("id", "ID"))
        return "Rp. ${formatter.format(number)}"
    }

    fun generatePdf(
        context: Context,
        groupTitle: String,
        periodeText: String,
        logoUriString: String?,
        transactions: List<KasTransaction>,
        saveLocation: PdfSaveLocation = PdfSaveLocation.DOWNLOADS
    ): PdfExportResult? {
        val pdfDocument = PdfDocument()

        // Page info A4 dimensions at 72dpi: 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842
        val marginX = 20f
        val bottomMargin = 45f

        val colWidths = floatArrayOf(30f, 75f, 190f, 85f, 85f, 90f)
        val colAlignRight = booleanArrayOf(false, false, false, true, true, true)
        val headers = arrayOf("No", "Tanggal", "Uraian", "Pemasukan", "Pengeluaran", "Saldo")

        val paint = Paint().apply { isAntiAlias = true }

        var pageNumber = 1
        var page = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas: Canvas = page.canvas
        var currentY = 30f

        // 1. Draw Logo if available
        if (!logoUriString.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(logoUriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 60, 60, true)
                    canvas.drawBitmap(scaledBitmap, (pageWidth - 60) / 2f, currentY, paint)
                    currentY += 76f
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Title
        paint.style = Paint.Style.FILL
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#004D40")
        val titleText = groupTitle.ifBlank { "LAPORAN KEUANGAN" }.uppercase(Locale.getDefault())
        val titleWidth = paint.measureText(titleText)
        canvas.drawText(titleText, ((pageWidth - titleWidth) / 2f).coerceAtLeast(marginX), currentY, paint)
        currentY += 22f

        // 3. Periode
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.DKGRAY
        val subTitle = "Periode: $periodeText"
        val subTitleWidth = paint.measureText(subTitle)
        canvas.drawText(subTitle, ((pageWidth - subTitleWidth) / 2f).coerceAtLeast(marginX), currentY, paint)
        currentY += 16f

        // 4. Print Timestamp
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val timeStamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val timeText = "Dicetak pada tanggal: $timeStamp"
        val timeWidth = paint.measureText(timeText)
        canvas.drawText(timeText, ((pageWidth - timeWidth) / 2f).coerceAtLeast(marginX), currentY, paint)
        currentY += 15f

        // 5. Separator Double Line
        paint.color = Color.parseColor("#004D40")
        paint.strokeWidth = 2f
        canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, paint)
        currentY += 4f
        paint.strokeWidth = 0.8f
        canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, paint)
        currentY += 20f

        fun drawTableHeader(targetCanvas: Canvas, yStart: Float): Float {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#E0F2F1")
            targetCanvas.drawRect(RectF(marginX, yStart, pageWidth - marginX, yStart + 24f), paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            var xPos = marginX
            for (i in headers.indices) {
                val cellWidth = colWidths[i]
                val text = headers[i]

                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#004D40")
                if (colAlignRight[i]) {
                    val textW = paint.measureText(text)
                    targetCanvas.drawText(text, xPos + cellWidth - textW - 5f, yStart + 16f, paint)
                } else {
                    targetCanvas.drawText(text, xPos + 5f, yStart + 16f, paint)
                }

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.5f
                paint.color = Color.GRAY
                targetCanvas.drawRect(RectF(xPos, yStart, xPos + cellWidth, yStart + 24f), paint)

                xPos += cellWidth
            }
            return yStart + 24f
        }

        // 6. Table Headers on Page 1
        currentY = drawTableHeader(canvas, currentY)

        // 7. Table Content Rows (with pagination support)
        var runningSaldo = 0L
        var totalMasuk = 0L
        var totalKeluar = 0L
        val rowHeight = 22f

        for ((index, item) in transactions.withIndex()) {
            if (currentY + rowHeight > pageHeight - bottomMargin) {
                pdfDocument.finishPage(page)
                pageNumber++
                page = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = page.canvas
                currentY = 35f
                currentY = drawTableHeader(canvas, currentY)
            }

            runningSaldo += (item.masuk - item.keluar)
            totalMasuk += item.masuk
            totalKeluar += item.keluar

            if (index % 2 == 1) {
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#F7FBFB")
                canvas.drawRect(RectF(marginX, currentY, pageWidth - marginX, currentY + rowHeight), paint)
            }

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            val rowValues = arrayOf(
                (index + 1).toString(),
                item.tanggal,
                if (item.uraian.length > 38) item.uraian.take(35) + "..." else item.uraian,
                formatRupiah(item.masuk),
                formatRupiah(item.keluar),
                formatRupiah(runningSaldo)
            )

            var xPos = marginX
            for (i in rowValues.indices) {
                val cellWidth = colWidths[i]
                val valText = rowValues[i]

                paint.style = Paint.Style.FILL
                paint.color = Color.BLACK
                paint.textSize = 9f

                if (colAlignRight[i]) {
                    val textW = paint.measureText(valText)
                    canvas.drawText(valText, xPos + cellWidth - textW - 5f, currentY + 15f, paint)
                } else {
                    canvas.drawText(valText, xPos + 5f, currentY + 15f, paint)
                }

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.5f
                paint.color = Color.LTGRAY
                canvas.drawRect(RectF(xPos, currentY, xPos + cellWidth, currentY + rowHeight), paint)

                xPos += cellWidth
            }
            currentY += rowHeight
        }

        // 8. Summary Footer Row (start new page if not enough space)
        val footerHeight = 26f
        if (currentY + footerHeight > pageHeight - bottomMargin) {
            pdfDocument.finishPage(page)
            pageNumber++
            page = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            canvas = page.canvas
            currentY = 35f
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E0F2F1")
        canvas.drawRect(RectF(marginX, currentY, pageWidth - marginX, currentY + footerHeight), paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.parseColor("#004D40")
        canvas.drawRect(RectF(marginX, currentY, pageWidth - marginX, currentY + footerHeight), paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#004D40")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val firstThreeWidth = colWidths[0] + colWidths[1] + colWidths[2]
        canvas.drawText("SALDO AKHIR", marginX + 10f, currentY + 17f, paint)

        val tmText = formatRupiah(totalMasuk)
        val tmW = paint.measureText(tmText)
        val tmX = marginX + firstThreeWidth
        canvas.drawText(tmText, tmX + colWidths[3] - tmW - 5f, currentY + 17f, paint)

        val tkText = formatRupiah(totalKeluar)
        val tkW = paint.measureText(tkText)
        val tkX = tmX + colWidths[3]
        canvas.drawText(tkText, tkX + colWidths[4] - tkW - 5f, currentY + 17f, paint)

        val saldoText = formatRupiah(totalMasuk - totalKeluar)
        val sW = paint.measureText(saldoText)
        val sX = tkX + colWidths[4]
        canvas.drawText(saldoText, sX + colWidths[5] - sW - 5f, currentY + 17f, paint)

        pdfDocument.finishPage(page)

        // Build safe file name
        val safeTitle = groupTitle.trim()
            .replace(Regex("[^a-zA-Z0-9._-]+"), "_")
            .trim('_')
            .ifBlank { "Laporan_Keuangan" }
        val timeStampFile = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Financial_Report_${safeTitle}_$timeStampFile.pdf"

        // Save internal cache file (used by FileProvider for sharing and SAF copy)
        val cacheDir = File(context.cacheDir, "reports")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val cacheFile = File(cacheDir, fileName)

        return try {
            FileOutputStream(cacheFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            // Save to public Download / Documents directory
            val (publicUri, displayPath, savedToPublic) = saveToPublicDirectory(
                context = context,
                sourceFile = cacheFile,
                fileName = fileName,
                saveLocation = saveLocation
            )

            PdfExportResult(
                cacheFile = cacheFile,
                publicUri = publicUri,
                displayPath = displayPath,
                fileName = fileName,
                savedToPublicStorage = savedToPublic
            )
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                pdfDocument.close()
            } catch (_: Exception) {
            }
            null
        }
    }

    private fun saveToPublicDirectory(
        context: Context,
        sourceFile: File,
        fileName: String,
        saveLocation: PdfSaveLocation
    ): Triple<Uri?, String, Boolean> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val baseDir = when (saveLocation) {
                PdfSaveLocation.DOWNLOADS -> Environment.DIRECTORY_DOWNLOADS
                PdfSaveLocation.DOCUMENTS -> Environment.DIRECTORY_DOCUMENTS
            }
            val relativePath = "$baseDir/$SUBFOLDER_NAME"
            val collectionUri = when (saveLocation) {
                PdfSaveLocation.DOWNLOADS -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
                PdfSaveLocation.DOCUMENTS -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            var insertedUri: Uri? = null
            try {
                insertedUri = resolver.insert(collectionUri, contentValues)
                if (insertedUri != null) {
                    resolver.openOutputStream(insertedUri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    } ?: throw IllegalStateException("Failed to open MediaStore output stream")

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(insertedUri, contentValues, null, null)

                    return Triple(insertedUri, "$relativePath/$fileName", true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (insertedUri != null) {
                    try {
                        resolver.delete(insertedUri, null, null)
                    } catch (_: Exception) {
                    }
                }
            }
        }

        // Fallback for API 24-28 (or if MediaStore insert fails): save in app external Documents dir
        return try {
            val baseEnv = when (saveLocation) {
                PdfSaveLocation.DOWNLOADS -> Environment.DIRECTORY_DOWNLOADS
                PdfSaveLocation.DOCUMENTS -> Environment.DIRECTORY_DOCUMENTS
            }
            val appExternalDir = File(context.getExternalFilesDir(baseEnv), SUBFOLDER_NAME)
            if (!appExternalDir.exists()) appExternalDir.mkdirs()
            val destFile = File(appExternalDir, fileName)
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Triple(null, destFile.absolutePath, false)
        } catch (e: Exception) {
            e.printStackTrace()
            Triple(null, sourceFile.absolutePath, false)
        }
    }

    fun copyFileToUri(context: Context, sourceFile: File, destUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            } != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
