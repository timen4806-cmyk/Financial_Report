package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.KasTransaction
import com.example.utils.PdfExporter
import com.example.utils.PdfSaveLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Financial Reports", appName)
  }

  @Test
  fun `generatePdf saves valid pdf file for Download and Documents`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sample = listOf(
      KasTransaction(id = 1, tanggal = "2026-11-11", uraian = "Saldo Awal", masuk = 5000000, keluar = 0),
      KasTransaction(id = 2, tanggal = "2026-11-11", uraian = "Biaya Operasional", masuk = 0, keluar = 250000)
    )

    val downloadResult = PdfExporter.generatePdf(
      context = context,
      groupTitle = "Laporan Kas",
      periodeText = "November 2026",
      logoUriString = null,
      transactions = sample,
      saveLocation = PdfSaveLocation.DOWNLOADS
    )
    assertNotNull(downloadResult)
    assertTrue(downloadResult!!.cacheFile.exists())
    assertTrue(downloadResult.cacheFile.length() > 0L)
    assertTrue(downloadResult.fileName.endsWith(".pdf"))

    val docsResult = PdfExporter.generatePdf(
      context = context,
      groupTitle = "Laporan Kas",
      periodeText = "November 2026",
      logoUriString = null,
      transactions = sample,
      saveLocation = PdfSaveLocation.DOCUMENTS
    )
    assertNotNull(docsResult)
    assertTrue(docsResult!!.cacheFile.exists())
    assertTrue(docsResult.cacheFile.length() > 0L)
  }
}
