package com.example.multibaggerbot.ui.components

import androidx.compose.foundation.background
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
import kotlin.math.max

@Composable
fun PositionSizingDialog(
    initialCapital: Double = 500000.0,
    presetTicker: String = "",
    presetCmp: Double = 1000.0,
    presetStopLoss: Double = 850.0,
    presetTarget: Double = 2000.0,
    onDismiss: () -> Unit
) {
    var capitalInput by remember { mutableStateOf(initialCapital.toInt().toString()) }
    var riskPct by remember { mutableStateOf(1.5) } // 1.5% default risk
    var entryPriceInput by remember { mutableStateOf(String.format("%.1f", presetCmp)) }
    var stopLossInput by remember { mutableStateOf(String.format("%.1f", presetStopLoss)) }
    var targetInput by remember { mutableStateOf(String.format("%.1f", presetTarget)) }

    val capital = capitalInput.toDoubleOrNull() ?: initialCapital
    val entryPrice = entryPriceInput.toDoubleOrNull() ?: presetCmp
    val stopLoss = stopLossInput.toDoubleOrNull() ?: presetStopLoss
    val target = targetInput.toDoubleOrNull() ?: presetTarget

    // Risk Calculations
    val maxRupeesAtRisk = capital * (riskPct / 100.0)
    val riskPerShare = max(0.5, entryPrice - stopLoss)
    val stopLossPct = if (entryPrice > 0) ((entryPrice - stopLoss) / entryPrice) * 100.0 else 0.0

    val maxShares = if (riskPerShare > 0) (maxRupeesAtRisk / riskPerShare).toInt().coerceAtLeast(1) else 1
    val totalInvestment = maxShares * entryPrice
    val portfolioAllocationPct = if (capital > 0) (totalInvestment / capital) * 100.0 else 0.0

    val rewardPerShare = max(0.0, target - entryPrice)
    val potentialProfit = maxShares * rewardPerShare
    val targetGainPct = if (entryPrice > 0) ((target - entryPrice) / entryPrice) * 100.0 else 0.0
    val riskRewardRatio = if (riskPerShare > 0) rewardPerShare / riskPerShare else 3.0

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⚖️ Position Sizing Calculator",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (presetTicker.isNotBlank()) {
                            Text(
                                text = "Evaluating: ${presetTicker.replace(".NS", "")}",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Row 1: Capital & Risk %
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = capitalInput,
                        onValueChange = { capitalInput = it },
                        label = { Text("Account Size (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Risk per Trade", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = "${String.format("%.1f", riskPct)}% (₹${String.format("%,.0f", maxRupeesAtRisk)})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8A80)
                        )
                        Slider(
                            value = riskPct.toFloat(),
                            onValueChange = { riskPct = it.toDouble() },
                            valueRange = 0.5f..5.0f,
                            steps = 8
                        )
                    }
                }

                // Row 2: Entry, Stop Loss, Target
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = entryPriceInput,
                        onValueChange = { entryPriceInput = it },
                        label = { Text("Entry Price (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stopLossInput,
                        onValueChange = { stopLossInput = it },
                        label = { Text("Stop Loss (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it },
                        label = { Text("Target (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Mathematical Position Sizing Output Card
                Surface(
                    color = Color(0xFF141722),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Recommended Share Quantity:", fontSize = 12.sp, color = Color.LightGray)
                            Text(
                                text = "$maxShares shares",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4CAF50)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Capital Outlay:", fontSize = 12.sp, color = Color.LightGray)
                            Text(
                                text = "₹${String.format("%,.0f", totalInvestment)} (${String.format("%.1f", portfolioAllocationPct)}% portfolio)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Risk per Share:", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = "₹${String.format("%,.1f", riskPerShare)} (-${String.format("%.1f", stopLossPct)}%)",
                                fontSize = 11.sp,
                                color = Color(0xFFEF5350)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Potential Reward:", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = "+₹${String.format("%,.0f", potentialProfit)} (+${String.format("%.1f", targetGainPct)}%)",
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = Color(0xFF222634))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Risk to Reward Ratio:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF90CAF9))
                            Text(
                                text = "1 : ${String.format("%.2f", riskRewardRatio)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (riskRewardRatio >= 3.0) Color(0xFF4CAF50) else Color(0xFFFFB300)
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply & Close")
                }
            }
        }
    }
}
