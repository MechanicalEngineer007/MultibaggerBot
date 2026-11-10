package com.example.multibaggerbot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multibaggerbot.data.Candle
import com.example.multibaggerbot.data.MultibaggerPick
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandlestickChartView(
    pick: MultibaggerPick,
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {}
) {
    var isWeeklyTimeframe by remember { mutableStateOf(false) }

    // Multi-Timeframe Candle Aggregation
    val displayCandles = remember(candles, isWeeklyTimeframe) {
        if (candles.isEmpty()) emptyList()
        else if (!isWeeklyTimeframe) {
            if (candles.size > 50) candles.takeLast(50) else candles
        } else {
            // Group daily candles into 5-day weekly bars
            candles.chunked(5).map { weekChunk ->
                Candle(
                    date = weekChunk.first().date,
                    open = weekChunk.first().open,
                    high = weekChunk.maxOf { it.high },
                    low = weekChunk.minOf { it.low },
                    close = weekChunk.last().close,
                    volume = weekChunk.sumOf { it.volume }
                )
            }.takeLast(35)
        }
    }

    if (displayCandles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Insufficient candle history to render chart.", color = Color.Gray)
        }
        return
    }

    val minPrice = min(
        displayCandles.minOf { it.low },
        min(pick.trailingStopLoss, pick.cmp)
    ) * 0.96
    val maxPrice = max(
        displayCandles.maxOf { it.high },
        max(pick.target1Year * 1.05, pick.fiftyTwoWeekHigh)
    ) * 1.03
    val priceRange = max(1.0, maxPrice - minPrice)

    // 30-Week (approx 150-Day) Moving Average for Weekly Stage-2 Inspector
    val weeklySma30 = remember(displayCandles, isWeeklyTimeframe) {
        if (isWeeklyTimeframe && displayCandles.size >= 10) {
            displayCandles.map { it.close }.average()
        } else pick.sma150
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${pick.symbol.replace(".NS", "")} • ${pick.stageStatus}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "CMP: ₹${String.format("%,.1f", pick.cmp)} | 1Y Target: ₹${String.format("%,.1f", pick.target1Year)} (+${pick.target1YearPct}%)",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onClose) {
                    Text("✕", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timeframe Toggle Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isWeeklyTimeframe,
                    onClick = { isWeeklyTimeframe = false },
                    label = {
                        Text(
                            text = "1D Daily (Tactical Entry)",
                            fontSize = 11.sp,
                            fontWeight = if (!isWeeklyTimeframe) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                FilterChip(
                    selected = isWeeklyTimeframe,
                    onClick = { isWeeklyTimeframe = true },
                    label = {
                        Text(
                            text = "1W Weekly (30-W Weinstein Stage-2)",
                            fontSize = 11.sp,
                            fontWeight = if (isWeeklyTimeframe) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFF141722), RoundedCornerShape(10.dp))
            ) {
                val width = size.width
                val height = size.height
                val numCandles = displayCandles.size
                val candleWidth = (width / numCandles) * 0.65f
                val candleSpacing = width / numCandles

                fun priceToY(price: Double): Float {
                    return (height - ((price - minPrice) / priceRange) * height).toFloat()
                }

                val target1YY = priceToY(pick.target1Year)
                val stopY = priceToY(pick.trailingStopLoss)
                val primaryMaY = priceToY(if (isWeeklyTimeframe) weeklySma30 else pick.sma200)

                // 1Y Target Green Dashed Line
                drawLine(
                    color = Color(0xFF4CAF50),
                    start = Offset(0f, target1YY),
                    end = Offset(width, target1YY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                // 200-SMA / 30-Week SMA Line
                val maColor = if (isWeeklyTimeframe) Color(0xFFCE93D8) else Color(0xFFFFB300)
                drawLine(
                    color = maColor,
                    start = Offset(0f, primaryMaY),
                    end = Offset(width, primaryMaY),
                    strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )

                // Stop Loss Red Dashed Line
                drawLine(
                    color = Color(0xFFEF5350),
                    start = Offset(0f, stopY),
                    end = Offset(width, stopY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )

                // Draw Candles
                for (i in displayCandles.indices) {
                    val c = displayCandles[i]
                    val x = i * candleSpacing + (candleSpacing / 2)
                    val isGreen = c.close >= c.open
                    val color = if (isGreen) Color(0xFF26A69A) else Color(0xFFEF5350)

                    val highY = priceToY(c.high)
                    val lowY = priceToY(c.low)
                    val openY = priceToY(c.open)
                    val closeY = priceToY(c.close)

                    // Wick
                    drawLine(
                        color = color,
                        start = Offset(x, highY),
                        end = Offset(x, lowY),
                        strokeWidth = 1.8f
                    )

                    // Body
                    val topY = min(openY, closeY)
                    val bottomY = max(openY, closeY)
                    val bodyHeight = max(2.5f, bottomY - topY)

                    drawRect(
                        color = color,
                        topLeft = Offset(x - (candleWidth / 2), topY),
                        size = Size(candleWidth, bodyHeight)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🎯 1Y Tgt: ₹${String.format("%,.0f", pick.target1Year)}", fontSize = 11.sp, color = Color(0xFF4CAF50))
                Text(
                    text = if (isWeeklyTimeframe) "💜 30-W SMA: ₹${String.format("%,.0f", weeklySma30)}" else "📈 200-SMA: ₹${String.format("%,.0f", pick.sma200)}",
                    fontSize = 11.sp,
                    color = if (isWeeklyTimeframe) Color(0xFFCE93D8) else Color(0xFFFFB300)
                )
                Text("🛡️ Stop: ₹${String.format("%,.0f", pick.trailingStopLoss)}", fontSize = 11.sp, color = Color(0xFFEF5350))
            }
        }
    }
}
