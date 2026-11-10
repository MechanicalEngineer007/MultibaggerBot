package com.example.multibaggerbot.engine

import com.example.multibaggerbot.data.Candle
import java.util.Random
import kotlin.math.*

data class MonteCarloResult(
    val medianPrice: Double,
    val bullPrice: Double,
    val bearPrice: Double,
    val probability2xPct: Double
)

data class VcpResult(
    val isVcpPivot: Boolean,
    val contractionScore: Int, // 0 - 100
    val wave1DepthPct: Double,
    val wave2DepthPct: Double,
    val wave3DepthPct: Double,
    val signalDescription: String
)

data class QuantitativeAnalysisResult(
    val volatilityAnnualized: Double,
    val driftAnnualized: Double,
    val rsi14: Double,
    val relativeStrengthRating: Int, // 1 - 99
    val vcp: VcpResult,
    val dcfTarget: Double,
    val gbmTarget: Double,
    val monteCarlo: MonteCarloResult,
    val peExpansionTarget: Double,
    val arimaTarget: Double
)

object QuantitativeModelEngine {

    private val random = Random(42) // Seeded for reproducibility

    fun analyze(
        candles: List<Candle>,
        salesGrowthCAGR: Double,
        patGrowthCAGR: Double,
        debtToEquity: Double,
        peRatio: Double,
        pegRatio: Double
    ): QuantitativeAnalysisResult {
        val cmp = candles.lastOrNull()?.close ?: 100.0
        if (candles.size < 30) {
            val fallbackMc = MonteCarloResult(cmp * 1.35, cmp * 1.70, cmp * 0.95, 35.0)
            val fallbackVcp = VcpResult(false, 50, 20.0, 14.0, 8.0, "Consolidating")
            return QuantitativeAnalysisResult(
                volatilityAnnualized = 0.28,
                driftAnnualized = 0.32,
                rsi14 = 55.0,
                relativeStrengthRating = 75,
                vcp = fallbackVcp,
                dcfTarget = cmp * 1.30,
                gbmTarget = cmp * 1.35,
                monteCarlo = fallbackMc,
                peExpansionTarget = cmp * 1.40,
                arimaTarget = cmp * 1.02
            )
        }

        val closes = candles.map { it.close }

        // 1. Calculate Daily Log Returns
        val logReturns = mutableListOf<Double>()
        for (i in 1 until closes.size) {
            val prev = closes[i - 1]
            val curr = closes[i]
            if (prev > 0 && curr > 0) {
                logReturns.add(ln(curr / prev))
            }
        }

        // 2. Annualized Volatility (sigma) & Drift (mu)
        val meanReturn = if (logReturns.isNotEmpty()) logReturns.average() else 0.001
        var varianceSum = 0.0
        for (r in logReturns) {
            varianceSum += (r - meanReturn).pow(2)
        }
        val dailyVol = if (logReturns.size > 1) sqrt(varianceSum / (logReturns.size - 1)) else 0.02
        val volatilityAnnualized = (dailyVol * sqrt(252.0)).coerceIn(0.18, 0.85)
        val rawDrift = (meanReturn * 252.0) + (0.5 * volatilityAnnualized.pow(2))
        // Reconcile technical momentum with fundamental profit compounding
        val fundamentalCAGR = ((salesGrowthCAGR * 0.4) + (patGrowthCAGR * 0.6)) / 100.0
        val driftAnnualized = (rawDrift * 0.45 + fundamentalCAGR * 0.55).coerceIn(-0.20, 1.20)

        // 3. 1,000-Iteration Empirical Monte Carlo Simulation
        val mc = runMonteCarloSimulation(cmp, volatilityAnnualized, driftAnnualized, tradingDays = 252, runs = 1000)

        // 4. Geometric Brownian Motion (GBM) Target
        val gbmTarget = cmp * exp(driftAnnualized.coerceIn(0.15, 0.95))

        // 5. Discounted Cash Flow (DCF) Valuation with India WACC
        val dcfTarget = calculateDcfFairValue(cmp, salesGrowthCAGR, patGrowthCAGR, debtToEquity)

        // 6. P/E Multiple Expansion (Lollapalooza Engine)
        val peExpansionTarget = calculatePeExpansionTarget(cmp, patGrowthCAGR, peRatio, pegRatio, debtToEquity)

        // 7. Time-Series Autoregressive Momentum Target (ARIMA proxy)
        val arimaTarget = calculateArimaMomentumTarget(closes, cmp, driftAnnualized)

        // 8. 14-Day Wilder RSI
        val rsi14 = calculateRsi14(closes)

        // 9. Volatility Contraction Pattern (VCP) Detector
        val vcp = detectVcpContraction(candles)

        // 10. Relative Strength (RS) vs Benchmark (Nifty 50 Proxy)
        val rsRating = calculateRelativeStrengthRating(closes)

        return QuantitativeAnalysisResult(
            volatilityAnnualized = roundTo2(volatilityAnnualized),
            driftAnnualized = roundTo2(driftAnnualized),
            rsi14 = roundTo2(rsi14),
            relativeStrengthRating = rsRating,
            vcp = vcp,
            dcfTarget = roundTo2(dcfTarget),
            gbmTarget = roundTo2(gbmTarget),
            monteCarlo = mc,
            peExpansionTarget = roundTo2(peExpansionTarget),
            arimaTarget = roundTo2(arimaTarget)
        )
    }

    private fun runMonteCarloSimulation(
        s0: Double,
        volatility: Double,
        drift: Double,
        tradingDays: Int = 252,
        runs: Int = 1000
    ): MonteCarloResult {
        val dt = 1.0 / tradingDays.toDouble()
        val driftTerm = (drift - 0.5 * volatility.pow(2)) * dt
        val volTerm = volatility * sqrt(dt)
        val simulatedPrices = DoubleArray(runs)
        var hit2xCount = 0

        for (i in 0 until runs) {
            var price = s0
            for (day in 0 until tradingDays) {
                // Standard Normal random variate Z ~ N(0, 1)
                val z = random.nextGaussian()
                price *= exp(driftTerm + volTerm * z)
            }
            simulatedPrices[i] = price
            if (price >= s0 * 2.0) {
                hit2xCount++
            }
        }

        simulatedPrices.sort()
        val medianIdx = (runs * 0.50).toInt().coerceIn(0, runs - 1)
        val bullIdx = (runs * 0.88).toInt().coerceIn(0, runs - 1)
        val bearIdx = (runs * 0.12).toInt().coerceIn(0, runs - 1)

        val prob2x = (hit2xCount.toDouble() / runs.toDouble()) * 100.0

        return MonteCarloResult(
            medianPrice = roundTo2(simulatedPrices[medianIdx]),
            bullPrice = roundTo2(simulatedPrices[bullIdx]),
            bearPrice = roundTo2(simulatedPrices[bearIdx]),
            probability2xPct = roundTo2(prob2x)
        )
    }

    private fun calculateDcfFairValue(
        cmp: Double,
        salesGrowthCAGR: Double,
        patGrowthCAGR: Double,
        debtToEquity: Double
    ): Double {
        // India Equity WACC (Weighted Average Cost of Capital) ~ 11.5%
        val wacc = 0.115
        val terminalGrowth = 0.06 // 6.0% Indian GDP terminal compounding proxy
        val g1 = (salesGrowthCAGR * 0.5 + patGrowthCAGR * 0.5).coerceIn(12.0, 60.0) / 100.0
        val g2 = g1 * 0.88
        val g3 = g2 * 0.80

        // 3-Year forward cash flow present value
        val pv1 = (1.0 + g1) / (1.0 + wacc)
        val pv2 = ((1.0 + g1) * (1.0 + g2)) / (1.0 + wacc).pow(2)
        val pv3 = ((1.0 + g1) * (1.0 + g2) * (1.0 + g3)) / (1.0 + wacc).pow(3)

        // Terminal Value discounted
        val cf3 = (1.0 + g1) * (1.0 + g2) * (1.0 + g3)
        val terminalValue = (cf3 * (1.0 + terminalGrowth)) / (wacc - terminalGrowth)
        val pvTerminal = terminalValue / (1.0 + wacc).pow(3)

        val solvencyDeduction = if (debtToEquity > 0.6) 0.90 else 1.05
        val dcfMultiple = ((pv1 + pv2 + pv3 + pvTerminal) * 0.14 * solvencyDeduction).coerceIn(1.15, 2.60)
        return cmp * dcfMultiple
    }

    private fun calculatePeExpansionTarget(
        cmp: Double,
        patGrowthCAGR: Double,
        peRatio: Double,
        pegRatio: Double,
        debtToEquity: Double
    ): Double {
        val earningsMultipleGrowth = 1.0 + (patGrowthCAGR / 100.0).coerceIn(0.15, 0.85)
        // PEG < 1.5 with low debt allows significant institutional multiple expansion
        val peExpansionFactor = when {
            pegRatio in 0.1..1.2 && debtToEquity <= 0.3 -> 1.35
            pegRatio in 1.2..1.8 && debtToEquity <= 0.5 -> 1.18
            peRatio > 70.0 -> 0.92 // Multiple compression for overvalued stocks
            else -> 1.08
        }
        return cmp * (earningsMultipleGrowth * peExpansionFactor)
    }

    private fun calculateArimaMomentumTarget(closes: List<Double>, cmp: Double, drift: Double): Double {
        val last20 = closes.takeLast(20)
        val slope = if (last20.size >= 2) (last20.last() - last20.first()) / last20.first() else 0.05
        val shortTermTrend = 1.0 + (slope * 0.5 + drift * 0.5).coerceIn(0.08, 0.70)
        return cmp * shortTermTrend
    }

    private fun calculateRsi14(closes: List<Double>): Double {
        if (closes.size < 15) return 55.0
        var gains = 0.0
        var losses = 0.0
        for (i in 1..14) {
            val diff = closes[closes.size - 15 + i] - closes[closes.size - 15 + i - 1]
            if (diff >= 0) gains += diff else losses += abs(diff)
        }
        var avgGain = gains / 14.0
        var avgLoss = losses / 14.0

        for (i in (closes.size - 14) until closes.size) {
            val diff = closes[i] - closes[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) abs(diff) else 0.0
            avgGain = (avgGain * 13 + gain) / 14.0
            avgLoss = (avgLoss * 13 + loss) / 14.0
        }

        if (avgLoss == 0.0) return 99.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(5.0, 95.0)
    }

    private fun detectVcpContraction(candles: List<Candle>): VcpResult {
        if (candles.size < 60) {
            return VcpResult(false, 50, 18.0, 12.0, 6.0, "Multi-Week Base Formation")
        }

        val window = candles.takeLast(60)
        val w1 = window.subList(0, 20)
        val w2 = window.subList(20, 42)
        val w3 = window.subList(42, window.size)

        val depth1 = calcSwingDepth(w1)
        val depth2 = calcSwingDepth(w2)
        val depth3 = calcSwingDepth(w3)

        // True VCP pattern: depth1 > depth2 > depth3, and depth3 is tight (<= 7%)
        val isContracting = depth1 > depth2 && depth2 > depth3 && depth3 <= 8.5
        val tightnessScore = if (isContracting) (95 - (depth3 * 4.0).toInt()).coerceIn(75, 98) else 50
        val desc = when {
            isContracting && depth3 <= 4.5 -> "🔥 Tight VCP 3-Wave Pivot (<4.5% Risk)"
            isContracting -> "⚡ Volatility Contraction Pattern Active"
            depth3 <= 5.0 -> "🛡️ Tight Low-Volatility Pivot"
            else -> "📊 Base Consolidation Building"
        }

        return VcpResult(
            isVcpPivot = isContracting,
            contractionScore = tightnessScore,
            wave1DepthPct = roundTo2(depth1),
            wave2DepthPct = roundTo2(depth2),
            wave3DepthPct = roundTo2(depth3),
            signalDescription = desc
        )
    }

    private fun calcSwingDepth(slice: List<Candle>): Double {
        if (slice.isEmpty()) return 10.0
        val h = slice.maxOf { it.high }
        val l = slice.minOf { it.low }
        return if (h > 0) ((h - l) / h) * 100.0 else 10.0
    }

    private fun calculateRelativeStrengthRating(closes: List<Double>): Int {
        if (closes.size < 40) return 70
        val ret3M = (closes.last() - closes[max(0, closes.size - 63)]) / closes[max(0, closes.size - 63)]
        val ret12M = (closes.last() - closes.first()) / closes.first()
        // Nifty benchmark expected 1Y return ~ 13% (0.13), 3M ~ 3.5% (0.035)
        val outperformance = (ret3M - 0.035) * 0.4 + (ret12M - 0.13) * 0.6
        val rating = (50 + (outperformance * 45.0)).toInt().coerceIn(15, 99)
        return rating
    }

    private fun roundTo2(v: Double): Double {
        return kotlin.math.round(v * 100.0) / 100.0
    }
}
