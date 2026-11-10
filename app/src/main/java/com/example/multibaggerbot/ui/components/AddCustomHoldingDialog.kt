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

@Composable
fun AddCustomHoldingDialog(
    title: String = "➕ Add Custom Stock Holding",
    isForSimulator: Boolean = false,
    onDismiss: () -> Unit,
    onAdd: (symbol: String, entryPrice: Double, shares: Int, stopLoss: Double, targetPrice: Double) -> Unit
) {
    var symbolInput by remember { mutableStateOf("") }
    var entryPriceInput by remember { mutableStateOf("") }
    var sharesInput by remember { mutableStateOf("10") }
    var stopLossInput by remember { mutableStateOf("") }
    var targetPriceInput by remember { mutableStateOf("") }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Track your personal Zerodha/Groww holdings against the bot's multibagger models.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = symbolInput,
                    onValueChange = { symbolInput = it.uppercase() },
                    label = { Text("NSE Stock Symbol") },
                    placeholder = { Text("e.g. TATAPOWER, TRENT, INFY, BEL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = entryPriceInput,
                        onValueChange = {
                            entryPriceInput = it
                            val p = it.toDoubleOrNull() ?: 0.0
                            if (p > 0) {
                                if (stopLossInput.isBlank()) stopLossInput = String.format("%.1f", p * 0.85)
                                if (targetPriceInput.isBlank()) targetPriceInput = String.format("%.1f", p * 2.0)
                            }
                        },
                        label = { Text("Your Entry / Buy Price (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )

                    OutlinedTextField(
                        value = sharesInput,
                        onValueChange = { sharesInput = it },
                        label = { Text("Quantity (Shares)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stopLossInput,
                        onValueChange = { stopLossInput = it },
                        label = { Text("200-DMA Stop Floor (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = targetPriceInput,
                        onValueChange = { targetPriceInput = it },
                        label = { Text("1Y 2x Target (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val cleanSym = symbolInput.trim().replace(" ", "").uppercase()
                            val sym = if (cleanSym.endsWith(".NS") || cleanSym.endsWith(".BO")) cleanSym else "$cleanSym.NS"
                            val entry = entryPriceInput.toDoubleOrNull() ?: 100.0
                            val shares = sharesInput.toIntOrNull() ?: 1
                            val stop = stopLossInput.toDoubleOrNull() ?: (entry * 0.85)
                            val target = targetPriceInput.toDoubleOrNull() ?: (entry * 2.0)

                            if (cleanSym.isNotBlank()) {
                                onAdd(sym, entry, shares, stop, target)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add to Tracker")
                    }
                }
            }
        }
    }
}
