package com.example.ui.dialogs

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.export.DatabaseBackupPayload
import com.example.data.export.ExportFormat
import com.example.data.export.ExportStatus
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.NavyCardBackground
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySlate800
import com.example.ui.theme.NavySlate900
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter
import com.example.util.DatabaseBackupSerializer
import com.example.util.TransactionCsvExporter

/**
 * Dialog for exporting data from AYNAAS Finance:
 * 1. JSON Format: Complete Room Database backup (Accounts, Transactions, Stock Holdings)
 * 2. CSV Format: Transaction history spreadsheet compatible with Excel, Google Sheets, LibreOffice Calc
 */
@Composable
fun ExportBackupDialog(
    initialFormat: ExportFormat = ExportFormat.JSON,
    payload: DatabaseBackupPayload?,
    jsonString: String?,
    exportStatus: ExportStatus,
    transactions: List<TransactionItem> = emptyList(),
    csvString: String? = null,
    csvExportStatus: ExportStatus = ExportStatus.Idle,
    onDismiss: () -> Unit,
    onSaveToUri: (android.net.Uri, String) -> Unit,
    onSaveToLocalStorage: () -> Unit,
    onShare: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onReload: () -> Unit,
    onPrepareCsv: () -> Unit = {},
    onSaveCsvToUri: (android.net.Uri, String) -> Unit = { _, _ -> },
    onSaveCsvToLocalStorage: () -> Unit = {},
    onShareCsv: () -> Unit = {},
    onCopyCsvToClipboard: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(initialFormat) }
    var showPreview by remember { mutableStateOf(false) }

    // When switching to CSV tab, ensure CSV string is generated
    LaunchedEffect(selectedFormat) {
        if (selectedFormat == ExportFormat.CSV && csvString == null) {
            onPrepareCsv()
        }
    }

    val defaultJsonFileName = remember(payload) {
        DatabaseBackupSerializer.generateBackupFileName(payload?.exportTimestampMillis ?: System.currentTimeMillis())
    }

    val defaultCsvFileName = remember {
        TransactionCsvExporter.generateCsvFileName()
    }

    // Android Storage Access Framework (SAF) Document Creator Launcher for JSON
    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onSaveToUri(uri, defaultJsonFileName)
        }
    }

    // Android Storage Access Framework (SAF) Document Creator Launcher for CSV
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            onSaveCsvToUri(uri, defaultCsvFileName)
        }
    }

    val dialogScrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp)
            .testTag("dialog_export_backup"),
        containerColor = NavyCardBackground,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeonCyan.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (selectedFormat == ExportFormat.JSON) Icons.Default.SaveAlt else Icons.Default.TableChart,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (selectedFormat == ExportFormat.JSON) "Cadangan Database (JSON)" else "Ekspor Riwayat Transaksi (CSV)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (selectedFormat == ExportFormat.JSON)
                                "Pencadangan offline-first Room Database lengkap"
                            else
                                "Format spreadsheet untuk Excel & Google Sheets",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Format Selector Segmented Control (JSON vs CSV)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NavySlate900,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // JSON Tab
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedFormat == ExportFormat.JSON) NeonCyan else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedFormat = ExportFormat.JSON }
                                .testTag("tab_export_json")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.SaveAlt,
                                    contentDescription = null,
                                    tint = if (selectedFormat == ExportFormat.JSON) NavyCardBackground else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "JSON Database",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedFormat == ExportFormat.JSON) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedFormat == ExportFormat.JSON) NavyCardBackground else TextSecondary
                                )
                            }
                        }

                        // CSV Tab
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedFormat == ExportFormat.CSV) NeonCyan else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedFormat = ExportFormat.CSV
                                    if (csvString == null) {
                                        onPrepareCsv()
                                    }
                                }
                                .testTag("tab_export_csv")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = if (selectedFormat == ExportFormat.CSV) NavyCardBackground else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CSV Transaksi",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedFormat == ExportFormat.CSV) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedFormat == ExportFormat.CSV) NavyCardBackground else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(dialogScrollState)
            ) {
                val activeStatus = if (selectedFormat == ExportFormat.JSON) exportStatus else csvExportStatus

                // Status Banner (Success, InProgress, Failure)
                when (activeStatus) {
                    is ExportStatus.InProgress -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NavyCardElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (selectedFormat == ExportFormat.JSON)
                                        "Menyimpan berkas cadangan database..."
                                    else
                                        "Menyiapkan berkas CSV transaksi...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    is ExportStatus.Success -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ProfitGreenBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("export_success_banner")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeStatus.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen
                                    )
                                }
                                if (!activeStatus.filePath.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Berkas: ${activeStatus.fileName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    is ExportStatus.Failure -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ExpenseRedBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("export_failure_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Ekspor Gagal",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                    Text(
                                        text = activeStatus.errorMessage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (selectedFormat == ExportFormat.JSON) onReload() else onPrepareCsv()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("Coba", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    ExportStatus.Idle -> { /* No banner */ }
                }

                if (selectedFormat == ExportFormat.JSON) {
                    // === JSON DATABASE BACKUP VIEW ===
                    RenderJsonBackupContent(
                        payload = payload,
                        jsonString = jsonString,
                        defaultFileName = defaultJsonFileName,
                        showPreview = showPreview,
                        onTogglePreview = { showPreview = !showPreview },
                        onSaveSaf = { createJsonLauncher.launch(defaultJsonFileName) },
                        onSaveLocalStorage = onSaveToLocalStorage,
                        onShare = onShare,
                        onCopyToClipboard = {
                            onCopyToClipboard()
                            Toast.makeText(context, "JSON berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    // === CSV SPREADSHEET EXPORT VIEW ===
                    RenderCsvExportContent(
                        transactions = transactions,
                        csvString = csvString,
                        defaultFileName = defaultCsvFileName,
                        showPreview = showPreview,
                        onTogglePreview = { showPreview = !showPreview },
                        onSaveSaf = { createCsvLauncher.launch(defaultCsvFileName) },
                        onSaveLocalStorage = onSaveCsvToLocalStorage,
                        onShare = onShareCsv,
                        onCopyToClipboard = {
                            onCopyCsvToClipboard()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_export_dialog")
            ) {
                Text(
                    text = "Tutup",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun RenderJsonBackupContent(
    payload: DatabaseBackupPayload?,
    jsonString: String?,
    defaultFileName: String,
    showPreview: Boolean,
    onTogglePreview: () -> Unit,
    onSaveSaf: () -> Unit,
    onSaveLocalStorage: () -> Unit,
    onShare: () -> Unit,
    onCopyToClipboard: () -> Unit
) {
    // Database Summary Metric Pills
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NavyCardElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Ringkasan Data Cadangan:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricPill(
                    icon = Icons.Default.AccountBalance,
                    label = "Rekening",
                    value = "${payload?.totalAccounts ?: 0}"
                )
                SummaryMetricPill(
                    icon = Icons.Default.ReceiptLong,
                    label = "Transaksi",
                    value = "${payload?.totalTransactions ?: 0}"
                )
                SummaryMetricPill(
                    icon = Icons.Default.ShowChart,
                    label = "Portofolio",
                    value = "${payload?.totalStockHoldings ?: 0}"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySlate900, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ukuran Data JSON:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    val sizeKb = (jsonString?.toByteArray(Charsets.UTF_8)?.size ?: 0) / 1024.0
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f KB", sizeKb),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nama berkas default:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = defaultFileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Action 1: Save via Android Storage Access Framework (SAF)
    Button(
        onClick = onSaveSaf,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("btn_export_save_saf"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NeonCyan,
            contentColor = NavyCardBackground
        )
    ) {
        Icon(
            Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Simpan ke Berkas (.json)",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Action 2: Quick Save to Local App Documents Directory
    OutlinedButton(
        onClick = onSaveLocalStorage,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("btn_export_save_local"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Simpan Cepat ke Penyimpanan Lokal",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Actions 3 & 4: Share & Copy
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .testTag("btn_export_share"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Bagikan",
                style = MaterialTheme.typography.labelMedium
            )
        }

        OutlinedButton(
            onClick = onCopyToClipboard,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .testTag("btn_export_copy"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Salin JSON",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Collapsible JSON Preview Section
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = NavySlate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pratinjau Format JSON",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
                TextButton(
                    onClick = onTogglePreview,
                    modifier = Modifier.testTag("btn_toggle_json_preview")
                ) {
                    Text(
                        text = if (showPreview) "Sembunyikan" else "Buka Pratinjau",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = showPreview,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val horizontalScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavyDeep)
                        .padding(8.dp)
                        .horizontalScroll(horizontalScrollState)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = jsonString ?: "Memuat data JSON...",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NeonCyan.copy(alpha = 0.9f),
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderCsvExportContent(
    transactions: List<TransactionItem>,
    csvString: String?,
    defaultFileName: String,
    showPreview: Boolean,
    onTogglePreview: () -> Unit,
    onSaveSaf: () -> Unit,
    onSaveLocalStorage: () -> Unit,
    onShare: () -> Unit,
    onCopyToClipboard: () -> Unit
) {
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    // CSV Transaction Summary Card
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NavyCardElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Data Siap Diekspor ke Spreadsheet:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ProfitGreenBg
                ) {
                    Text(
                        text = "RFC 4180 CSV",
                        color = ProfitGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricPill(
                    icon = Icons.Default.ReceiptLong,
                    label = "Total Transaksi",
                    value = "${transactions.size}"
                )
                SummaryMetricPill(
                    icon = Icons.Default.Description,
                    label = "Pemasukan",
                    value = CurrencyFormatter.formatCompactAxis(totalIncome)
                )
                SummaryMetricPill(
                    icon = Icons.Default.TableChart,
                    label = "Pengeluaran",
                    value = CurrencyFormatter.formatCompactAxis(totalExpense)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compatibility description box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NavySlate900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "✓ Kolom: Tanggal, Waktu, Tipe, Kategori, Nominal (IDR), Rekening, Catatan",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "✓ Kompatibel dengan Microsoft Excel, Google Spreadsheet, Apple Numbers, & Calc",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nama berkas default:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = defaultFileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Action 1: Save via Android Storage Access Framework (SAF)
    Button(
        onClick = onSaveSaf,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("btn_export_csv_saf"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NeonCyan,
            contentColor = NavyCardBackground
        )
    ) {
        Icon(
            Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Simpan Berkas Spreadsheet (.csv)",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Action 2: Quick Save to Local App Documents Directory
    OutlinedButton(
        onClick = onSaveLocalStorage,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("btn_export_csv_local"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Simpan ke Folder Dokumen Aplikasi",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Actions 3 & 4: Share & Copy
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .testTag("btn_export_csv_share"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Bagikan CSV",
                style = MaterialTheme.typography.labelMedium
            )
        }

        OutlinedButton(
            onClick = onCopyToClipboard,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .testTag("btn_export_csv_copy"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Salin Teks",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Collapsible CSV Preview Section
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = NavySlate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TableChart,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pratinjau Data CSV",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
                TextButton(
                    onClick = onTogglePreview,
                    modifier = Modifier.testTag("btn_toggle_csv_preview")
                ) {
                    Text(
                        text = if (showPreview) "Sembunyikan" else "Buka Pratinjau",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = showPreview,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val horizontalScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavyDeep)
                        .padding(8.dp)
                        .horizontalScroll(horizontalScrollState)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = csvString ?: "Menyiapkan data CSV...",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NeonCyan.copy(alpha = 0.9f),
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricPill(
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = NavySlate800,
        modifier = Modifier.width(86.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
