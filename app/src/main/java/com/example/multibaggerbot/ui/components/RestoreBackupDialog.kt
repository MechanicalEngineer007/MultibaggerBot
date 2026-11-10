package com.example.multibaggerbot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.multibaggerbot.data.BackupRestoreHelper
import com.example.multibaggerbot.data.FullAppBackup

@Composable
fun RestoreBackupDialog(
    onDismiss: () -> Unit,
    onRestoreConfirmed: (backup: FullAppBackup) -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var parsedBackup by remember { mutableStateOf<FullAppBackup?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2230),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📥 Restore Portfolio from Backup",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", color = Color.Gray, fontSize = 16.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFF2E3448))

                Text(
                    text = "Paste the JSON content of your exported .json backup file below to restore your positions, simulation configs, and watchlists.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = {
                        jsonInput = it
                        val b = BackupRestoreHelper.parseBackupJson(it)
                        parsedBackup = b
                        parseError = if (it.isNotBlank() && b == null) "Invalid backup JSON format." else null
                    },
                    label = { Text("Backup JSON Content") },
                    placeholder = { Text("Paste { \"version\": \"1.0.0\", ... } here") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                parseError?.let {
                    Text("⚠️ $it", color = Color(0xFFEF5350), fontSize = 11.sp)
                }

                parsedBackup?.let { b ->
                    Surface(
                        color = Color(0xFF183222),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("✅ Valid Backup Verified!", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("• Audit Positions: ${b.auditPositions.size} holdings", fontSize = 11.sp, color = Color.White)
                            Text("• Simulation Scenarios: ${b.simulationConfigs.size} configs", fontSize = 11.sp, color = Color.White)
                            Text("• Watchlist: ${b.watchlist.size} stocks", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            parsedBackup?.let {
                                onRestoreConfirmed(it)
                                onDismiss()
                            }
                        },
                        enabled = parsedBackup != null,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Restore Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
