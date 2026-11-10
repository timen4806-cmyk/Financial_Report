package com.example.utils

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
import com.example.data.KasTransaction
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun formatRupiah(number: Long): String {
        val formatter = NumberFormat.getInstance(Locale("id", "ID"))
        return "Rp. ${formatter.format(number)}"
    }

    fun generatePdf(
        context: Context,
        groupTitle: String,
        periodeText: String,
        logoUriString: String?,
        transactions: List<KasTransaction>
    ): File? {
        val pdfDocument = PdfDocument()

        // Page info A4 dimensions at 72dpi: 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        paint.isAntiAlias = true

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
                    currentY += 70f
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Title
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#004D40")
        val titleText = groupTitle.uppercase(Locale.getDefault())
        val titleWidth = paint.measureText(titleText)
        canvas.drawText(titleText, (pageWidth - titleWidth) / 2f, currentY, paint)
        currentY += 22f

        // 3. Periode
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.DKGRAY
        val subTitle = "Periode: $periodeText"
        val subTitleWidth = paint.measureText(subTitle)
        canvas.drawText(subTitle, (pageWidth - subTitleWidth) / 2f, currentY, paint)
        currentY += 16f

        // 4. Print Timestamp
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val timeStamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val timeText = "Dicetak pada tanggal: $timeStamp"
        val timeWidth = paint.measureText(timeText)
        canvas.drawText(timeText, (pageWidth - timeWidth) / 2f, currentY, paint)
        currentY += 15f

        // 5. Separator Double Line
        paint.color = Color.parseColor("#004D40")
        paint.strokeWidth = 2f
        canvas.drawLine(20f, currentY, pageWidth - 20f, currentY, paint)
        currentY += 4f
        paint.strokeWidth = 0.8f
        canvas.drawLine(20f, currentY, pageWidth - 20f, currentY, paint)
        currentY += 20f

        // 6. Table Headers
        // Column widths (Total width = 555)
        val colWidths = floatArrayOf(30f, 75f, 190f, 85f, 85f, 90f)
        val colAlignRight = booleanArrayOf(false, false, false, true, true, true)
        val headers = arrayOf("No", "Tanggal", "Uraian", "Pemasukan", "Pengeluaran", "Saldo")
        val marginX = 20f

        // Header Background
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E0F2F1")
        canvas.drawRect(RectF(marginX, currentY, pageWidth - marginX, currentY + 24f), paint)

        // Header Text & Borders
        paint.color = Color.parseColor("#004D40")
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
                canvas.drawText(text, xPos + cellWidth - textW - 5f, currentY + 16f, paint)
            } else {
                canvas.drawText(text, xPos + 5f, currentY + 16f, paint)
            }

            // Cell border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            paint.color = Color.GRAY
            canvas.drawRect(RectF(xPos, currentY, xPos + cellWidth, currentY + 24f), paint)

            xPos += cellWidth
        }
        currentY += 24f

        // 7. Table Content Rows
        var runningSaldo = 0L
        var totalMasuk = 0L
        var totalKeluar = 0L

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        for ((index, item) in transactions.withIndex()) {
            runningSaldo += (item.masuk - item.keluar)
            totalMasuk += item.masuk
            totalKeluar += item.keluar

            val rowHeight = 22f

            // Alternate row background
            if (index % 2 == 1) {
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#F7FBFB")
                canvas.drawRect(RectF(marginX, currentY, pageWidth - marginX, currentY + rowHeight), paint)
            }

            val rowValues = arrayOf(
                (index + 1).toString(),
                item.tanggal,
                if (item.uraian.length > 38) item.uraian.take(35) + "..." else item.uraian,
                formatRupiah(item.masuk),
                formatRupiah(item.keluar),
                formatRupiah(runningSaldo)
            )

            xPos = marginX
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

                // Border
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.5f
                paint.color = Color.LTGRAY
                canvas.drawRect(RectF(xPos, currentY, xPos + cellWidth, currentY + rowHeight), paint)

                xPos += cellWidth
            }
            currentY += rowHeight

            // Simple check to prevent overflow
            if (currentY > pageHeight - 60) {
                break
            }
        }

        // 8. Summary Footer Row
        val footerHeight = 26f
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E0F2F1")
        canvas.drawRect(RectF(marginX, currentY, pageWidth - marginX, currentY + footerHeight), paint)

        // Footer Border
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

        // Total Masuk
        val tmText = formatRupiah(totalMasuk)
        val tmW = paint.measureText(tmText)
        val tmX = marginX + firstThreeWidth
        canvas.drawText(tmText, tmX + colWidths[3] - tmW - 5f, currentY + 17f, paint)

        // Total Keluar
        val tkText = formatRupiah(totalKeluar)
        val tkW = paint.measureText(tkText)
        val tkX = tmX + colWidths[3]
        canvas.drawText(tkText, tkX + colWidths[4] - tkW - 5f, currentY + 17f, paint)

        // Saldo Akhir Total
        val saldoText = formatRupiah(totalMasuk - totalKeluar)
        val sW = paint.measureText(saldoText)
        val sX = tkX + colWidths[4]
        canvas.drawText(saldoText, sX + colWidths[5] - sW - 5f, currentY + 17f, paint)

        pdfDocument.finishPage(page)

        // Save File
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()

        val timeStampFile = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val pdfFile = File(dir, "Financial_Report_${groupTitle.replace(" ", "_")}_$timeStampFile.pdf")

        return try {
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
