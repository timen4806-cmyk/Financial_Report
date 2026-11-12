package com.example.ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.utils.PdfExportResult
import com.example.utils.PdfExporter
import com.example.utils.PdfSaveLocation
import com.example.utils.ShareUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfPreviewDialog(
    activeGroup: String,
    totalMasuk: Long,
    totalKeluar: Long,
    saldoAkhir: Long,
    itemCount: Int,
    onGeneratePdf: (customTitle: String, periodeText: String, saveLocation: PdfSaveLocation) -> PdfExportResult?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var judulLaporan by remember { mutableStateOf(activeGroup) }
    var periodeText by remember {
        mutableStateOf(SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date()))
    }
    var selectedLocation by remember { mutableStateOf(PdfSaveLocation.DOWNLOADS) }
    var exportResult by remember { mutableStateOf<PdfExportResult?>(null) }
    var customSavedMessage by remember { mutableStateOf<String?>(null) }

    // System SAF Save-As launcher for custom folder or Android < 10 fallback
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        val pendingFile = exportResult?.cacheFile
        if (targetUri != null && pendingFile != null) {
            val copied = PdfExporter.copyFileToUri(context, pendingFile, targetUri)
            if (copied) {
                customSavedMessage = "Berhasil disimpan ke folder pilihan Anda."
                Toast.makeText(context, "PDF berhasil disimpan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal menyimpan file PDF.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        },
        title = {
            Text(
                text = "Cetak, Simpan & Share PDF",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = judulLaporan,
                    onValueChange = { judulLaporan = it },
                    label = { Text("Judul Laporan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_title_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = periodeText,
                    onValueChange = { periodeText = it },
                    label = { Text("Periode (Bulan / Tahun)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_periode_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Folder Destination Picker (Download or Documents)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Folder Penyimpanan Internal HP:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfSaveLocation.entries.forEach { location ->
                            FilterChip(
                                selected = selectedLocation == location,
                                onClick = { selectedLocation = location },
                                label = { Text(location.label) },
                                leadingIcon = if (selectedLocation == location) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier.testTag("folder_chip_${location.name.lowercase()}")
                            )
                        }
                    }
                    Text(
                        text = "Lokasi: ${selectedLocation.folderDisplay}/",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Summary Card inside dialog
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Ringkasan PDF:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Group: $activeGroup ($itemCount Transaksi)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Saldo Akhir: ${PdfExporter.formatRupiah(saldoAkhir)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                exportResult?.let { result ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PDF tersimpan di:\n${result.displayPath}",
                                color = Color(0xFF1B5E20),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            customSavedMessage?.let { msg ->
                                Text(
                                    text = msg,
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        ShareUtils.openPdfFile(context, result.cacheFile, result.publicUri)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_pdf_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Buka PDF", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = {
                                        createDocumentLauncher.launch(result.fileName)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_as_pdf_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Folder Lain", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        customSavedMessage = null
                        val result = onGeneratePdf(judulLaporan, periodeText, selectedLocation)
                        exportResult = result
                        if (result != null) {
                            if (result.savedToPublicStorage) {
                                Toast.makeText(
                                    context,
                                    "PDF disimpan ke ${result.displayPath}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                createDocumentLauncher.launch(result.fileName)
                            } else {
                                Toast.makeText(
                                    context,
                                    "PDF dibuat di ${result.displayPath}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(context, "Gagal membuat file PDF.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_pdf_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SIMPAN PDF KE ${selectedLocation.label.uppercase()}", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        customSavedMessage = null
                        val result = onGeneratePdf(judulLaporan, periodeText, selectedLocation)
                        exportResult = result
                        if (result != null) {
                            ShareUtils.sharePdfFile(context, result.cacheFile)
                        } else {
                            Toast.makeText(context, "Gagal membuat file PDF.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_pdf_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SIMPAN & BAGIKAN PDF", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ShareUtils.shareSummaryText(
                                context = context,
                                groupTitle = judulLaporan,
                                totalMasuk = totalMasuk,
                                totalKeluar = totalKeluar,
                                saldoAkhir = saldoAkhir,
                                itemCount = itemCount
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_summary_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Ringkasan")
                    }

                    OutlinedButton(
                        onClick = {
                            ShareUtils.openWhatsAppMessage(
                                context = context,
                                groupTitle = judulLaporan
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_wa_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Share WA", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
