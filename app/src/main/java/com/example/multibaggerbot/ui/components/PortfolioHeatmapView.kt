package com.example.multibaggerbot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multibaggerbot.data.ActiveAuditPosition

@Composable
fun PortfolioHeatmapView(
    auditPositions: List<ActiveAuditPosition>,
    onSelectHolding: (ActiveAuditPosition) -> Unit = {}
) {
    if (auditPositions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No active positions to display in heatmap.", color = Color.Gray, fontSize = 14.sp)
        }
        return
    }

    // Capital & Sector calculations
    val totalInvested = auditPositions.sumOf { if (it.investedCapital > 0) it.investedCapital else 50000.0 }
    val totalCurrent = auditPositions.sumOf {
        val invested = if (it.investedCapital > 0) it.investedCapital else 50000.0
        invested * (1.0 + (it.pnlPct / 100.0))
    }
    val totalGrossGain = totalCurrent - totalInvested
    val totalGainPct = if (totalInvested > 0) (totalGrossGain / totalInvested) * 100.0 else 0.0

    val totalEstTax = auditPositions.sumOf { it.estimatedTax }
    val totalPostTaxGain = totalGrossGain - totalEstTax

    // Sector breakdown
    val sectorGroups = auditPositions.groupBy { it.sector.ifBlank { "Diversified" } }
    val sectorWeights = sectorGroups.map { (sector, items) ->
        val sectorCapital = items.sumOf { if (it.investedCapital > 0) it.investedCapital else 50000.0 }
        val pct = if (totalInvested > 0) (sectorCapital / totalInvested) * 100.0 else 0.0
        sector to pct
    }.sortedByDescending { it.second }

    val topSector = sectorWeights.firstOrNull()
    val isOverconcentrated = (topSector?.second ?: 0.0) > 35.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Post-Tax Net Profit Realization Banner
        Surface(
            color = Color(0xFF131A26),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏛️ POST-TAX IN-HAND REALIZATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90CAF9)
                    )
                    Text(
                        text = "India Budget 2024 Regime",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Gross Gain", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            "${if (totalGrossGain >= 0) "+" else ""}₹${String.format("%,.0f", totalGrossGain)} (${String.format("%.1f", totalGainPct)}%)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalGrossGain >= 0) Color(0xFF81C784) else Color(0xFFEF5350)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Est. Tax Liability", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            "₹${String.format("%,.0f", totalEstTax)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Net In-Hand Profit", fontSize = 10.sp, color = Color(0xFF81C784))
                        Text(
                            "₹${String.format("%,.0f", totalPostTaxGain)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }

        // Sector Diversification Bar & Concentration Risk Banner
        Surface(
            color = if (isOverconcentrated) Color(0xFF2A1C16) else Color(0xFF17202C),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOverconcentrated) "⚠️ SECTOR CONCENTRATION ALERT" else "🛡️ SECTOR DIVERSIFICATION OPTIMAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverconcentrated) Color(0xFFFF9800) else Color(0xFF81C784)
                    )
                    Text(
                        text = "Max Limit: 35%",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }

                if (isOverconcentrated && topSector != null) {
                    Text(
                        text = "'${topSector.first}' holds ${String.format("%.1f", topSector.second)}% of capital. Consider diversifying into defensive themes to curb drawdowns.",
                        fontSize = 10.sp,
                        color = Color(0xFFFFCC80)
                    )
                }

                // Sector Pills Scrollable Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val colors = listOf(
                        Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFF8E24AA),
                        Color(0xFFFB8C00), Color(0xFF00ACC1), Color(0xFFD81B60)
                    )
                    sectorWeights.forEachIndexed { idx, (sector, pct) ->
                        val chipColor = colors[idx % colors.size]
                        Surface(
                            color = chipColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, chipColor.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sector, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("${String.format("%.1f", pct)}%", fontSize = 10.sp, color = chipColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Treemap Heatmap Grid (Chunked Rows - Seamless Natural Scrolling)
        auditPositions.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { pos ->
                    Box(modifier = Modifier.weight(1f)) {
                        HeatmapTile(
                            pos = pos,
                            totalCapital = totalInvested,
                            onClick = { onSelectHolding(pos) }
                        )
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun HeatmapTile(
    pos: ActiveAuditPosition,
    totalCapital: Double,
    onClick: () -> Unit
) {
    val invested = if (pos.investedCapital > 0) pos.investedCapital else 50000.0
    val weightPct = if (totalCapital > 0) (invested / totalCapital) * 100.0 else 0.0
    val pnl = pos.pnlPct

    val isDump = pos.healthStatus == "DUMP_WARNING" || pos.dumpWarnings.any { it.contains("🚨") || it.contains("Stage-4") }
    val isCaution = pos.healthStatus == "WATCH_CAUTION" || pos.dumpWarnings.isNotEmpty()

    // Color gradient coding based on health status and return %
    val (bgColor, borderColor, textColor) = when {
        isDump -> Triple(Color(0xFF3B1515), Color(0xFFC62828), Color(0xFFEF9A9A))
        isCaution -> Triple(Color(0xFF332314), Color(0xFFF57C00), Color(0xFFFFB74D))
        pnl >= 25.0 -> Triple(Color(0xFF143820), Color(0xFF2E7D32), Color(0xFF81C784))
        pnl >= 0.0 -> Triple(Color(0xFF162A1F), Color(0xFF388E3C), Color(0xFFA5D6A7))
        else -> Triple(Color(0xFF3B1515), Color(0xFFC62828), Color(0xFFEF9A9A))
    }

    val taxRate = if (pos.holdingDays >= 365) "LTCG 12.5%" else "STCG 20%"

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = pos.ticker.replace(".NS", ""),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = pos.sector.take(16),
                        fontSize = 9.sp,
                        color = Color.LightGray
                    )
                }

                Surface(
                    color = borderColor.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${if (pnl >= 0) "+" else ""}${String.format("%.1f", pnl)}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CMP: ₹${String.format("%,.0f", pos.currentPrice)}", fontSize = 10.sp, color = Color.LightGray)
                Text("Weight: ${String.format("%.1f", weightPct)}%", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Invested: ₹${String.format("%,.0f", invested)}", fontSize = 9.sp, color = Color.Gray)
                Text(taxRate, fontSize = 9.sp, color = Color(0xFF90CAF9))
            }

            // Profit Ladder or Warning Milestone Badge
            val milestone = when {
                isDump -> "🚨 DUMP WARNING: EXIT / TRIM"
                isCaution -> "⚠️ WATCH / CAUTION: RISK"
                pnl >= 100.0 -> "🚀 2x Multibagger: Free Runner"
                pnl >= 35.0 -> "🎯 Tier-1: Lock 25% & Breakeven"
                pnl >= 0.0 -> "📈 Riding Stage-2 Trend"
                else -> "🛡️ Protection (SL: ₹${String.format("%,.0f", pos.stopLoss)})"
            }

            Surface(
                color = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = milestone,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
