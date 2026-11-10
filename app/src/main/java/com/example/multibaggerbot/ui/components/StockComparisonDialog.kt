package com.example.multibaggerbot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.multibaggerbot.data.MultibaggerPick

@Composable
fun StockComparisonDialog(
    stockA: MultibaggerPick,
    candidatePicks: List<MultibaggerPick>,
    onDismiss: () -> Unit
) {
    // Select Stock B (default to second pick in list or another prominent pick)
    var selectedStockB by remember {
        mutableStateOf(candidatePicks.firstOrNull { it.symbol != stockA.symbol } ?: stockA)
    }
    var showStockDropdown by remember { mutableStateOf(false) }

    val stockB = selectedStockB

    // Evaluate Head-to-Head points
    var scoreAWins = 0
    var scoreBWins = 0

    if (stockA.score >= stockB.score) scoreAWins++ else scoreBWins++
    if (stockA.mc2xProb >= stockB.mc2xProb) scoreAWins++ else scoreBWins++
    if (stockA.patGrowthCAGR >= stockB.patGrowthCAGR) scoreAWins++ else scoreBWins++
    if (stockA.salesGrowthCAGR >= stockB.salesGrowthCAGR) scoreAWins++ else scoreBWins++
    if (stockA.piotroskiFScore >= stockB.piotroskiFScore) scoreAWins++ else scoreBWins++
    if (stockA.altmanZScore >= stockB.altmanZScore) scoreAWins++ else scoreBWins++
    if (stockA.rsRating >= stockB.rsRating) scoreAWins++ else scoreBWins++

    // For PEG, lower positive is better
    val pegAValid = if (stockA.pegRatio in 0.1..5.0) stockA.pegRatio else 99.0
    val pegBValid = if (stockB.pegRatio in 0.1..5.0) stockB.pegRatio else 99.0
    if (pegAValid <= pegBValid) scoreAWins++ else scoreBWins++

    val totalPoints = 8
    val (winnerStock, winnerPoints, loserStock) = if (scoreAWins >= scoreBWins) {
        Triple(stockA, scoreAWins, stockB)
    } else {
        Triple(stockB, scoreBWins, stockA)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141824),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3852)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⚔️ Stock Comparison Matrix",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Institutional Factor-by-Factor Evaluation",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stock B Selector Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showStockDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF90CAF9))
                    ) {
                        Text(
                            text = "Comparing with: ${stockB.symbol.replace(".NS", "")} - ${stockB.companyName.take(18)} ▼",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    DropdownMenu(
                        expanded = showStockDropdown,
                        onDismissRequest = { showStockDropdown = false }
                    ) {
                        candidatePicks.forEach { pick ->
                            DropdownMenuItem(
                                text = {
                                    Text("${pick.symbol.replace(".NS", "")} • Score ${pick.score} • ₹${pick.cmp.toInt()}")
                                },
                                onClick = {
                                    selectedStockB = pick
                                    showStockDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Automated Institutional Winner Verdict Banner
                Surface(
                    color = Color(0xFF1B2E24),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏆 VERDICT: ${winnerStock.symbol.replace(".NS", "")} WINS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF81C784)
                            )
                            Text(
                                text = "$winnerPoints of $totalPoints Points",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA5D6A7)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${winnerStock.symbol.replace(".NS", "")} exhibits superior factor strength over ${loserStock.symbol.replace(".NS", "")} with higher Score (${winnerStock.score} vs ${loserStock.score}) and stronger growth metrics.",
                            fontSize = 10.sp,
                            color = Color(0xFFC8E6C9)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Column Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E2538), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("METRIC", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.3f))
                    Text(stockA.symbol.replace(".NS", ""), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF64B5F6), modifier = Modifier.weight(1f))
                    Text(stockB.symbol.replace(".NS", ""), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFB74D), modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Matrix Table
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ComparisonRow("Multibagger Score", "${stockA.score}/100", "${stockB.score}/100", stockA.score >= stockB.score)
                    ComparisonRow("Current Price (CMP)", "₹${String.format("%,.0f", stockA.cmp)}", "₹${String.format("%,.0f", stockB.cmp)}", null)
                    ComparisonRow("1-Year Target", "+${stockA.target1YearPct.toInt()}%", "+${stockB.target1YearPct.toInt()}%", stockA.target1YearPct >= stockB.target1YearPct)
                    ComparisonRow("2-Year Multibagger Target", "+${stockA.target2YearPct.toInt()}%", "+${stockB.target2YearPct.toInt()}%", stockA.target2YearPct >= stockB.target2YearPct)
                    ComparisonRow("Monte Carlo 2x Prob", "${String.format("%.1f", stockA.mc2xProb)}%", "${String.format("%.1f", stockB.mc2xProb)}%", stockA.mc2xProb >= stockB.mc2xProb)
                    ComparisonRow("PAT Growth CAGR", "${stockA.patGrowthCAGR}%", "${stockB.patGrowthCAGR}%", stockA.patGrowthCAGR >= stockB.patGrowthCAGR)
                    ComparisonRow("Sales Growth CAGR", "${stockA.salesGrowthCAGR}%", "${stockB.salesGrowthCAGR}%", stockA.salesGrowthCAGR >= stockB.salesGrowthCAGR)
                    ComparisonRow("Piotroski F-Score", "${stockA.piotroskiFScore}/9", "${stockB.piotroskiFScore}/9", stockA.piotroskiFScore >= stockB.piotroskiFScore)
                    ComparisonRow("Altman Z-Score", "${stockA.altmanZScore}", "${stockB.altmanZScore}", stockA.altmanZScore >= stockB.altmanZScore)
                    ComparisonRow("RS Rating (vs Nifty 50)", "${stockA.rsRating}/99", "${stockB.rsRating}/99", stockA.rsRating >= stockB.rsRating)
                    ComparisonRow("14-Day RSI", "${stockA.rsi14.toInt()}", "${stockB.rsi14.toInt()}", null)
                    ComparisonRow("VCP Contraction", if (stockA.isVcpContraction) "🔥 Tight" else "Standard", if (stockB.isVcpContraction) "🔥 Tight" else "Standard", stockA.isVcpContraction && !stockB.isVcpContraction)
                    ComparisonRow("P/E Ratio", "${stockA.peRatio}x", "${stockB.peRatio}x", stockA.peRatio <= stockB.peRatio)
                    ComparisonRow("PEG Ratio", "${stockA.pegRatio}", "${stockB.pegRatio}", pegAValid <= pegBValid)
                    ComparisonRow("ADTV Turnover", "₹${stockA.adtvCrores} Cr", "₹${stockB.adtvCrores} Cr", stockA.adtvCrores >= stockB.adtvCrores)
                    ComparisonRow("Promoter Holding", "${stockA.promoterHolding}%", "${stockB.promoterHolding}%", stockA.promoterHolding >= stockB.promoterHolding)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Close Comparison", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ComparisonRow(
    label: String,
    valA: String,
    valB: String,
    isAWinner: Boolean?
) {
    Surface(
        color = Color(0xFF181D2B),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.LightGray,
                modifier = Modifier.weight(1.3f)
            )

            Text(
                text = valA,
                fontSize = 11.sp,
                fontWeight = if (isAWinner == true) FontWeight.Bold else FontWeight.Normal,
                color = if (isAWinner == true) Color(0xFF81C784) else Color.White,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = valB,
                fontSize = 11.sp,
                fontWeight = if (isAWinner == false) FontWeight.Bold else FontWeight.Normal,
                color = if (isAWinner == false) Color(0xFF81C784) else Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
