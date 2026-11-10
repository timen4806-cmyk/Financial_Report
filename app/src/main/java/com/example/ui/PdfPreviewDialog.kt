package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.utils.PdfExporter
import com.example.utils.ShareUtils
import java.io.File
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
    onGeneratePdf: (periodeText: String) -> File?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var judulLaporan by remember { mutableStateOf(activeGroup) }
    var periodeText by remember {
        mutableStateOf(SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date()))
    }
    var generatedFile by remember { mutableStateOf<File?>(null) }

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
                text = "Cetak & Share Laporan Keuangan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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

                if (generatedFile != null) {
                    Text(
                        text = "File PDF berhasil dibuat!\n${generatedFile?.name}",
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
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
                        val file = onGeneratePdf(periodeText)
                        generatedFile = file
                        if (file != null) {
                            ShareUtils.sharePdfFile(context, file)
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
                    Text("PROSES & BAGIKAN PDF", fontWeight = FontWeight.Bold)
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
                Text("Batal")
            }
        }
    )
}
