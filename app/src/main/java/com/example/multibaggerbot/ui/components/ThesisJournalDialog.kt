package com.example.multibaggerbot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ThesisJournalDialog(
    ticker: String,
    companyName: String,
    initialNotes: String,
    onDismiss: () -> Unit,
    onSaveNotes: (newNotes: String) -> Unit
) {
    var notesText by remember { mutableStateOf(initialNotes) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2230),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📝 Investment Thesis & Journal",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${ticker.replace(".NS", "")} • $companyName",
                            fontSize = 12.sp,
                            color = Color(0xFF64B5F6),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", color = Color.Gray, fontSize = 16.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFF2E3448))

                // Institutional Thesis Prompts
                Surface(
                    color = Color(0xFF141824),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("💡 Multibagger Thesis Checklist:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9))
                        Text("• Why will EPS double over 1–2 years?", fontSize = 10.sp, color = Color.LightGray)
                        Text("• Key Catalysts: Order book, Capex completion, or Margin expansion?", fontSize = 10.sp, color = Color.LightGray)
                        Text("• Invalidation criteria: What fundamental event triggers an exit?", fontSize = 10.sp, color = Color.LightGray)
                    }
                }

                // Notes Text Area
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Personal Research Notes & Thesis Rationale") },
                    placeholder = { Text("e.g. Bought Stage-2 pivot after 40% PAT jump. Expecting order book expansion. Stop loss at 200-DMA...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick Thesis Preset Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf("Earnings Beat", "Capex Expansion", "Stage-2 Pivot")
                    presets.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF262C3D),
                            modifier = Modifier.weight(1f)
                        ) {
                            TextButton(onClick = {
                                notesText = if (notesText.isBlank()) tag else "$notesText | $tag"
                            }) {
                                Text(tag, fontSize = 9.sp, color = Color(0xFF90CAF9))
                            }
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
                            onSaveNotes(notesText.trim())
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Thesis", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
