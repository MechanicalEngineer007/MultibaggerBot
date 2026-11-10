package com.example.multibaggerbot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.pow
import kotlin.math.roundToLong

@Composable
fun SipAccumulatorDialog(
    presetStockName: String = "Multibagger Megatrend Basket",
    presetMonthlySip: Double = 15000.0,
    presetCagr: Double = 35.0,
    presetMonths: Int = 24,
    onDismiss: () -> Unit,
    onApplyToSimulator: ((monthlyAmount: Double, months: Int, expectedCagr: Double) -> Unit)? = null
) {
    var monthlySipInput by remember { mutableStateOf(presetMonthlySip.toInt().toString()) }
    var cagrPct by remember { mutableStateOf(presetCagr) }
    var durationMonths by remember { mutableStateOf(presetMonths) }

    val p = monthlySipInput.toDoubleOrNull() ?: presetMonthlySip
    val n = durationMonths.coerceAtLeast(1)
    val totalInvested = p * n

    // Monthly compounding formula: FV = P * [((1 + r)^n - 1) / r] * (1 + r)
    val r = (cagrPct / 100.0) / 12.0
    val futureValue = if (r > 0) {
        p * (((1.0 + r).pow(n.toDouble()) - 1.0) / r) * (1.0 + r)
    } else totalInvested

    val wealthGain = futureValue - totalInvested
    val absoluteGainPct = if (totalInvested > 0) (wealthGain / totalInvested) * 100.0 else 0.0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2230),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "💰 Multibagger SIP Accumulator",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = presetStockName,
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

                // Monthly SIP Input with Quick Selectors
                Column {
                    Text("MONTHLY ACCUMULATION (₹)", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = monthlySipInput,
                        onValueChange = { monthlySipInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickAmounts = listOf(5000, 10000, 25000, 50000)
                        quickAmounts.forEach { amt ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (p.toInt() == amt) MaterialTheme.colorScheme.primaryContainer else Color(0xFF262C3D),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { monthlySipInput = amt.toString() }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text("₹${amt / 1000}k", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Expected Annual CAGR Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("EXPECTED MULTIBAGGER CAGR", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Text("${cagrPct.toInt()}% p.a.", fontSize = 13.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = cagrPct.toFloat(),
                        onValueChange = { cagrPct = it.toDouble() },
                        valueRange = 15f..65f,
                        steps = 9
                    )
                }

                // Time Horizon Selection Chips
                Column {
                    Text("TIME HORIZON (MONTHS)", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val horizons = listOf(12 to "1 Year", 24 to "2 Years", 36 to "3 Years", 60 to "5 Years")
                        horizons.forEach { (m, label) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (durationMonths == m) Color(0xFF2E7D32) else Color(0xFF262C3D),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { durationMonths = m }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("${m}M", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(label, fontSize = 9.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }

                // Compounding Wealth Output Card
                Surface(
                    color = Color(0xFF141824),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Invested Outlay", fontSize = 11.sp, color = Color.Gray)
                            Text("₹${String.format("%,d", totalInvested.roundToLong())}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Wealth Created (Gain)", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                "+₹${String.format("%,d", wealthGain.roundToLong())} (+${String.format("%.1f", absoluteGainPct)}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784)
                            )
                        }

                        HorizontalDivider(color = Color(0xFF262D42))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ESTIMATED PORTFOLIO VALUE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9))
                            Text(
                                "₹${String.format("%,d", futureValue.roundToLong())}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }

                // Stage 1 Base Accumulation Checklist
                Surface(
                    color = Color(0xFF1B2332),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("💡 Stage-1 Systematic Accumulation Playbook", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9))
                        Text("• Accumulate each monthly tranches during 20-DMA or 50-DMA price pullbacks.", fontSize = 10.sp, color = Color.LightGray)
                        Text("• Never average down if stock closes below 200-DMA Stop Floor.", fontSize = 10.sp, color = Color.LightGray)
                        Text("• Let winners compound across the 1–2 year markup expansion cycle.", fontSize = 10.sp, color = Color.LightGray)
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
                        Text("Close", fontSize = 12.sp)
                    }

                    onApplyToSimulator?.let { callback ->
                        Button(
                            onClick = {
                                callback(p, n, cagrPct)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Apply to Basket", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
