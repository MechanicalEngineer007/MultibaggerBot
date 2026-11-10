package com.example.multibaggerbot.engine

import com.example.multibaggerbot.data.Candle
import kotlin.math.max
import kotlin.math.min

data class StageAnalysisResult(
    val isStage2: Boolean,
    val stageLabel: String,
    val technicalScore: Int,
    val sma50: Double,
    val sma150: Double,
    val sma200: Double,
    val fiftyTwoWeekHigh: Double,
    val fiftyTwoWeekLow: Double,
    val distFrom52WkHighPct: Double,
    val distAbove52WkLowPct: Double,
    val accumulationRatio: Double,
    val breakoutSignal: String
)

object StageAnalysisEngine {

    fun analyze(candles: List<Candle>): StageAnalysisResult {
        if (candles.size < 50) {
            val cmp = candles.lastOrNull()?.close ?: 0.0
            return StageAnalysisResult(
                isStage2 = false,
                stageLabel = "Insufficient Data",
                technicalScore = 40,
                sma50 = cmp,
                sma150 = cmp,
                sma200 = cmp,
                fiftyTwoWeekHigh = cmp,
                fiftyTwoWeekLow = cmp,
                distFrom52WkHighPct = 0.0,
                distAbove52WkLowPct = 0.0,
                accumulationRatio = 1.0,
                breakoutSignal = "Consolidation"
            )
        }

        val closes = candles.map { it.close }
        val lastIdx = closes.lastIndex
        val cmp = closes[lastIdx]

        // SMAs
        val sma50 = calculateSma(closes, 50, lastIdx)
        val sma150 = calculateSma(closes, 150, lastIdx)
        val sma200 = calculateSma(closes, 200, lastIdx)
        val sma200PastMonth = calculateSma(closes, 200, max(0, lastIdx - 20))

        // 52-Week (approx 250 trading days) High / Low
        val lookback52Wk = min(candles.size, 250)
        val window = candles.takeLast(lookback52Wk)
        val high52Wk = window.maxOfOrNull { it.high } ?: cmp
        val low52Wk = window.minOfOrNull { it.low } ?: (cmp * 0.5)

        val distFromHighPct = if (high52Wk > 0) ((high52Wk - cmp) / high52Wk) * 100.0 else 0.0
        val distAboveLowPct = if (low52Wk > 0) ((cmp - low52Wk) / low52Wk) * 100.0 else 0.0

        // Institutional Volume Accumulation (Ratio of volume on up days vs down days over last 50 days)
        val volWindow = candles.takeLast(min(50, candles.size))
        var upVolume = 0.0
        var downVolume = 0.0
        for (i in 1 until volWindow.size) {
            if (volWindow[i].close >= volWindow[i - 1].close) {
                upVolume += volWindow[i].volume
            } else {
                downVolume += volWindow[i].volume
            }
        }
        val accumulationRatio = if (downVolume > 0) (upVolume / downVolume) else 1.2

        // Minervini Trend Template Validation
        val priceAbove200 = cmp > sma200
        val priceAbove150 = cmp > sma150
        val sma150Above200 = sma150 > sma200
        val sma200TrendingUp = sma200 >= sma200PastMonth * 0.995 // 200 SMA flat or rising
        val sma50Above150 = sma50 > sma150
        val nearAth = distFromHighPct <= 25.0 // Within 25% of 52-week high
        val wellAboveAtl = distAboveLowPct >= 30.0 // At least 30% above 52-week low

        var score = 0
        if (priceAbove200) score += 20
        if (priceAbove150) score += 15
        if (sma150Above200) score += 15
        if (sma200TrendingUp) score += 15
        if (sma50Above150) score += 10
        if (nearAth) score += 15
        if (wellAboveAtl) score += 10

        val isStage2 = priceAbove200 && sma150Above200 && sma200TrendingUp && nearAth
        val stageLabel = when {
            isStage2 && distFromHighPct <= 10.0 -> "🚀 Stage-2 ATH Breakout"
            isStage2 -> "📈 Stage-2 Accumulation"
            priceAbove200 && distFromHighPct > 25.0 -> "🔄 Pullback in Long Uptrend"
            cmp < sma200 && distAboveLowPct < 20.0 -> "⚠️ Stage-4 Downtrend"
            else -> "🧱 Stage-1 Base Building"
        }

        val breakoutSignal = when {
            distFromHighPct <= 5.0 && accumulationRatio > 1.25 -> "🔥 Fresh 52W High Breakout (Heavy Accumulation)"
            distFromHighPct <= 12.0 && cmp > sma50 -> "⚡ VCP (Volatility Contraction) Pivot"
            cmp in (sma50 * 0.98)..(sma50 * 1.03) && sma50 > sma200 -> "🛡️ 50-DMA Key Support Bounce"
            else -> "📊 Multi-Month Base Consolidation"
        }

        return StageAnalysisResult(
            isStage2 = isStage2,
            stageLabel = stageLabel,
            technicalScore = score.coerceIn(20, 100),
            sma50 = sma50,
            sma150 = sma150,
            sma200 = sma200,
            fiftyTwoWeekHigh = high52Wk,
            fiftyTwoWeekLow = low52Wk,
            distFrom52WkHighPct = distFromHighPct,
            distAbove52WkLowPct = distAboveLowPct,
            accumulationRatio = accumulationRatio,
            breakoutSignal = breakoutSignal
        )
    }

    private fun calculateSma(data: List<Double>, period: Int, endIdx: Int): Double {
        if (data.isEmpty() || endIdx < 0) return 0.0
        val start = max(0, endIdx - period + 1)
        val slice = data.subList(start, endIdx + 1)
        return slice.average()
    }
}
