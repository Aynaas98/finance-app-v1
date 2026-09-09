package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.SavingsGoalEntity
import com.example.ui.components.SensorNominalText
import com.example.ui.theme.NavyCardBackground
import com.example.ui.theme.NavySlate800
import com.example.ui.theme.NavySlate900
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavingsGoalsScreen(
    goals: List<SavingsGoalEntity>,
    isBalanceVisible: Boolean,
    onAddGoal: (title: String, targetAmount: Double, currentAmount: Double, deadlineMillis: Long, categoryIcon: String, colorHex: Long, note: String) -> Unit,
    onUpdateGoal: (SavingsGoalEntity) -> Unit,
    onDeleteGoal: (SavingsGoalEntity) -> Unit,
    onAddFunds: (goalId: Long, amount: Double) -> Unit
) {
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var goalForDeposit by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    val totalTarget = goals.sumOf { it.targetAmount }
    val totalSaved = goals.sumOf { it.currentAmount }
    val overallProgress = if (totalTarget > 0.0) (totalSaved / totalTarget).coerceIn(0.0, 1.0) else 0.0

    Scaffold(
        containerColor = NavySlate900,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddDialogOpen = true },
                containerColor = NeonCyan,
                contentColor = NavyCardBackground,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_savings_goal")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Target Tabungan")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Overview Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySlate800),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Target Finansial",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                SensorNominalText(
                                    amount = totalTarget,
                                    isBalanceVisible = isBalanceVisible,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = NeonCyan.copy(alpha = 0.15f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Terkumpul",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                SensorNominalText(
                                    amount = totalSaved,
                                    isBalanceVisible = isBalanceVisible,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Pencapaian",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", overallProgress * 100)}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { overallProgress.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = NeonCyan,
                            trackColor = NavySlate900,
                        )
                    }
                }
            }

            // Section title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Target Tabungan (${goals.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            if (goals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum ada target tabungan",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ketuk tombol + untuk membuat target finansial baru",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(goals, key = { it.id }) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        isBalanceVisible = isBalanceVisible,
                        onDepositClick = { goalForDeposit = goal },
                        onEditClick = { goalToEdit = goal },
                        onDeleteClick = { onDeleteGoal(goal) },
                        onToggleComplete = {
                            onUpdateGoal(goal.copy(isCompleted = !goal.isCompleted))
                        }
                    )
                }
            }
        }
    }

    if (isAddDialogOpen) {
        AddOrEditSavingsGoalDialog(
            goalToEdit = null,
            onDismiss = { isAddDialogOpen = false },
            onConfirm = { title, target, current, deadline, icon, color, note ->
                onAddGoal(title, target, current, deadline, icon, color, note)
                isAddDialogOpen = false
            }
        )
    }

    goalToEdit?.let { goal ->
        AddOrEditSavingsGoalDialog(
            goalToEdit = goal,
            onDismiss = { goalToEdit = null },
            onConfirm = { title, target, current, deadline, icon, color, note ->
                onUpdateGoal(
                    goal.copy(
                        title = title,
                        targetAmount = target,
                        currentAmount = current,
                        deadlineMillis = deadline,
                        categoryIcon = icon,
                        colorHex = color,
                        note = note
                    )
                )
                goalToEdit = null
            }
        )
    }

    goalForDeposit?.let { goal ->
        AddFundsDialog(
            goal = goal,
            onDismiss = { goalForDeposit = null },
            onConfirm = { addedAmount ->
                onAddFunds(goal.id, addedAmount)
                goalForDeposit = null
            }
        )
    }
}

@Composable
fun SavingsGoalCard(
    goal: SavingsGoalEntity,
    isBalanceVisible: Boolean,
    onDepositClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val progress = if (goal.targetAmount > 0.0) (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0) else 0.0
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val deadlineDate = remember(goal.deadlineMillis) { dateFormat.format(Date(goal.deadlineMillis)) }
    
    val now = System.currentTimeMillis()
    val diffDays = ((goal.deadlineMillis - now) / (1000 * 60 * 60 * 24)).toInt()
    val deadlineStatusText = when {
        goal.isCompleted -> "Target Tercapai!"
        diffDays < 0 -> "Jatuh tempo terlewati"
        diffDays == 0 -> "Jatuh tempo hari ini"
        else -> "Sisa $diffDays hari lagi"
    }
    val deadlineStatusColor = if (goal.isCompleted) Color(0xFF34D399) else if (diffDays < 0) Color(0xFFEF4444) else TextSecondary

    val iconVector = when (goal.categoryIcon.lowercase()) {
        "shield" -> Icons.Default.Shield
        "flight" -> Icons.Default.Flight
        "laptop" -> Icons.Default.Laptop
        "home" -> Icons.Default.Home
        "car", "directions_car" -> Icons.Default.DirectionsCar
        "school" -> Icons.Default.School
        else -> Icons.Default.Flag
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySlate800),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(goal.colorHex).copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = Color(goal.colorHex),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tenggat: $deadlineDate ($deadlineStatusText)",
                            style = MaterialTheme.typography.bodySmall,
                            color = deadlineStatusColor,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleComplete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Tandai Selesai",
                            tint = if (goal.isCompleted) Color(0xFF34D399) else TextMuted
                        )
                    }
                    IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary)
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444))
                    }
                }
            }

            if (goal.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = goal.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Terkumpul",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    SensorNominalText(
                        amount = goal.currentAmount,
                        isBalanceVisible = isBalanceVisible,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    SensorNominalText(
                        amount = goal.targetAmount,
                        isBalanceVisible = isBalanceVisible,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (goal.isCompleted) Color(0xFF34D399) else NeonCyan,
                trackColor = NavySlate900,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", progress * 100)}% tercapai",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Button(
                    onClick = onDepositClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan.copy(alpha = 0.15f),
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Tambah Dana", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddOrEditSavingsGoalDialog(
    goalToEdit: SavingsGoalEntity?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetAmount: Double, currentAmount: Double, deadlineMillis: Long, categoryIcon: String, colorHex: Long, note: String) -> Unit
) {
    var title by remember { mutableStateOf(goalToEdit?.title ?: "") }
    var targetStr by remember { mutableStateOf(goalToEdit?.targetAmount?.let { if (it > 0) it.toLong().toString() else "" } ?: "") }
    var currentStr by remember { mutableStateOf(goalToEdit?.currentAmount?.let { if (it > 0) it.toLong().toString() else "" } ?: "0") }
    var note by remember { mutableStateOf(goalToEdit?.note ?: "") }
    var selectedIcon by remember { mutableStateOf(goalToEdit?.categoryIcon ?: "shield") }

    // Default deadline 6 months from now if new
    val defaultDeadline = goalToEdit?.deadlineMillis ?: (System.currentTimeMillis() + 180L * 24 * 60 * 60 * 1000)
    var deadlineDaysStr by remember {
        val days = ((defaultDeadline - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        mutableStateOf(days.toString())
    }

    val icons = listOf("shield", "flight", "laptop", "home", "car", "school", "flag")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySlate800,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(text = if (goalToEdit == null) "Buat Target Tabungan Baru" else "Edit Target Tabungan")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Target (cth: Dana Darurat)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextMuted,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Target Jumlah (IDR)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextMuted,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentStr,
                    onValueChange = { currentStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Sudah Terkumpul (IDR)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextMuted,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = deadlineDaysStr,
                    onValueChange = { deadlineDaysStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Estimasi Durasi (dalam Hari)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextMuted,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Pilih Ikon:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    icons.forEach { iconName ->
                        val isSelected = selectedIcon == iconName
                        val ic = when (iconName) {
                            "shield" -> Icons.Default.Shield
                            "flight" -> Icons.Default.Flight
                            "laptop" -> Icons.Default.Laptop
                            "home" -> Icons.Default.Home
                            "car" -> Icons.Default.DirectionsCar
                            "school" -> Icons.Default.School
                            else -> Icons.Default.Flag
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) NeonCyan else NavySlate900,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { selectedIcon = iconName },
                            shadowElevation = if (isSelected) 4.dp else 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ic,
                                    contentDescription = null,
                                    tint = if (isSelected) NavyCardBackground else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan / Deskripsi (Opsional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextMuted,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tAmount = targetStr.toDoubleOrNull() ?: 0.0
                    val cAmount = currentStr.toDoubleOrNull() ?: 0.0
                    val days = deadlineDaysStr.toLongOrNull() ?: 180L
                    val deadlineMillis = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000)
                    if (title.isNotBlank() && tAmount > 0.0) {
                        onConfirm(title, tAmount, cAmount, deadlineMillis, selectedIcon, 0xFF38BDF8L, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = NavyCardBackground)
            ) {
                Text(text = "Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AddFundsDialog(
    goal: SavingsGoalEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var depositStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySlate800,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(text = "Tambah Dana: ${goal.title}")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Masukkan jumlah nominal yang ingin ditambahkan ke target tabungan ini:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = depositStr,
                    onValueChange = { depositStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Jumlah Setoran (IDR)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextMuted,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = depositStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onConfirm(amt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = NavyCardBackground)
            ) {
                Text(text = "Tambah", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal", color = TextSecondary)
            }
        }
    )
}
