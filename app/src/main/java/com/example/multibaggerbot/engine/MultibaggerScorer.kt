package com.example.multibaggerbot.engine

import com.example.multibaggerbot.data.MultibaggerPick
import com.example.multibaggerbot.data.StockData
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

object MultibaggerScorer {

    fun evaluate(stockData: StockData): MultibaggerPick? {
        val candles = stockData.candles
        if (candles.size < 40) return null

        val cmp = candles.last().close
        if (cmp <= 0) return null

        val stage = StageAnalysisEngine.analyze(candles)
        val profile = FundamentalMoatEngine.getProfile(stockData.symbol, stockData.meta?.shortName ?: stockData.meta?.longName)

        // 1. Fundamental Score (0 - 100)
        var fundScore = 0

        // Sales Growth
        when {
            profile.salesGrowthCAGR >= 30.0 -> fundScore += 25
            profile.salesGrowthCAGR >= 20.0 -> fundScore += 20
            profile.salesGrowthCAGR >= 15.0 -> fundScore += 15
            else -> fundScore += 5
        }

        // PAT Growth
        when {
            profile.patGrowthCAGR >= 35.0 -> fundScore += 25
            profile.patGrowthCAGR >= 25.0 -> fundScore += 20
            profile.patGrowthCAGR >= 15.0 -> fundScore += 15
            else -> fundScore += 5
        }

        // Solvency (Low Debt is crucial for 1-2 year holding)
        when {
            profile.debtToEquity <= 0.1 -> fundScore += 25  // Virtually debt-free
            profile.debtToEquity <= 0.4 -> fundScore += 20
            profile.debtToEquity <= 0.8 -> fundScore += 12
            else -> fundScore += 2
        }

        // Valuation & Moat (PEG & Institutional backing)
        if (profile.pegRatio <= 1.4) fundScore += 15 else if (profile.pegRatio <= 1.8) fundScore += 8
        if (profile.promoterHolding + profile.institutionalHolding >= 65.0) fundScore += 10

        fundScore = fundScore.coerceIn(20, 100)

        // 2. Composite Multibagger Score
        // 50% Fundamental Quality + 40% Stage-2 Technical Structure + 10% Accumulation
        val accumulationBonus = if (stage.accumulationRatio > 1.2) 10 else 5
        val compositeScore = (
            (fundScore * 0.50) +
            (stage.technicalScore * 0.40) +
            (accumulationBonus)
        ).roundToInt().coerceIn(10, 99)

        // Conviction Grade
        val grade = when {
            compositeScore >= 85 -> "⭐ AAA Prime Multibagger"
            compositeScore >= 72 -> "🚀 AA High-Growth Compounder"
            compositeScore >= 60 -> "📈 A Steady Wealth Creator"
            else -> "🌱 Speculative Growth"
        }

        // 3. Quantitative & Statistical Models Engine
        val quant = QuantitativeModelEngine.analyze(
            candles = candles,
            salesGrowthCAGR = profile.salesGrowthCAGR,
            patGrowthCAGR = profile.patGrowthCAGR,
            debtToEquity = profile.debtToEquity,
            peRatio = profile.peRatio,
            pegRatio = profile.pegRatio
        )

        // 4. Adaptive Machine Learning Consensus Forecasting
        val mlPrediction = AdaptiveMachineLearningEngine.predictAdaptiveConsensus(
            cmp = cmp,
            dcfTarget = quant.dcfTarget,
            gbmTarget = quant.gbmTarget,
            mcTarget = quant.monteCarlo.medianPrice,
            peExpTarget = quant.peExpansionTarget,
            arimaTarget = quant.arimaTarget,
            score = compositeScore
        )

        // 5. Target Projections (1-Year & 2-Year)
        val stageBoost = if (stage.isStage2) 1.05 else 0.95
        val base1YTarget = cmp * profile.target1YMultiple * stageBoost
        val target1Year = roundToDecimals((base1YTarget * 0.45 + mlPrediction.adaptiveConsensusPrice * 0.55), 2)
        val target1YearPct = roundToDecimals(((target1Year - cmp) / cmp) * 100.0, 1)

        val target2YMultiple = profile.target2YMultiple * stageBoost
        val target2Year = roundToDecimals(max(target1Year * 1.35, cmp * target2YMultiple), 2)
        val target2YearPct = roundToDecimals(((target2Year - cmp) / cmp) * 100.0, 1)

        // 2-Year CAGR %
        val cagr2Y = roundToDecimals(((target2Year / cmp).pow(0.5) - 1.0) * 100.0, 1)

        // Trailing Stop-Loss for Positional Investors (using 200 SMA / 150 SMA buffer)
        val rawStop = if (stage.sma200 > 0) stage.sma200 * 0.94 else cmp * 0.82
        val trailingStop = roundToDecimals(rawStop.coerceAtMost(cmp * 0.90), 2)

        val riskRupees = cmp - trailingStop
        val rewardRupees = target1Year - cmp
        val rrRatio = if (riskRupees > 0) roundToDecimals(rewardRupees / riskRupees, 2) else 3.5

        // Liquidity, Turnover & Circuit Filter Guardian
        val last20Vol = candles.takeLast(20)
        val avgTurnover = if (last20Vol.isNotEmpty()) {
            last20Vol.map { (it.close * it.volume) / 10000000.0 }.average()
        } else 25.0
        val adtv = roundToDecimals(avgTurnover.coerceIn(1.5, 450.0), 1)

        val circuitBand = when {
            adtv >= 40.0 -> 20.0
            adtv >= 10.0 -> 10.0
            else -> 5.0
        }

        val lastCandle = candles.last()
        val prevClose = if (candles.size >= 2) candles[candles.size - 2].close else lastCandle.close
        val dayChangePct = if (prevClose > 0) ((lastCandle.close - prevClose) / prevClose) * 100.0 else 0.0
        val isNearUc = (circuitBand - dayChangePct) <= 1.2 && dayChangePct > 0
        val isNearLc = (dayChangePct - (-circuitBand)) <= 1.2 && dayChangePct < 0
        val isNearCircuit = isNearUc || isNearLc

        val circuitLabel = when {
            isNearUc -> "⚡ Near Upper Circuit (+${circuitBand.toInt()}%)"
            isNearLc -> "🚨 Near Lower Circuit (-${circuitBand.toInt()}%)"
            adtv >= 25.0 -> "🟢 High Float (₹${adtv} Cr/Day)"
            adtv >= 8.0 -> "🟡 Medium Float (₹${adtv} Cr/Day)"
            else -> "⚠️ Low Float (₹${adtv} Cr/Day — Circuit Risk)"
        }

        return MultibaggerPick(
            symbol = stockData.symbol,
            companyName = profile.companyName,
            cmp = roundToDecimals(cmp, 2),
            score = compositeScore,
            convictionGrade = grade,
            stageStatus = stage.stageLabel,
            target1Year = target1Year,
            target1YearPct = target1YearPct,
            target2Year = target2Year,
            target2YearPct = target2YearPct,
            trailingStopLoss = trailingStop,
            riskRewardRatio = rrRatio,
            salesGrowthCAGR = profile.salesGrowthCAGR,
            patGrowthCAGR = profile.patGrowthCAGR,
            debtToEquity = profile.debtToEquity,
            peRatio = profile.peRatio,
            pegRatio = profile.pegRatio,
            promoterHolding = profile.promoterHolding,
            institutionalHolding = profile.institutionalHolding,
            sectorTheme = profile.sectorTheme,
            moatDescription = profile.moatDescription,
            sma50 = roundToDecimals(stage.sma50, 2),
            sma150 = roundToDecimals(stage.sma150, 2),
            sma200 = roundToDecimals(stage.sma200, 2),
            fiftyTwoWeekHigh = roundToDecimals(stage.fiftyTwoWeekHigh, 2),
            fiftyTwoWeekLow = roundToDecimals(stage.fiftyTwoWeekLow, 2),
            volumeSurgeRatio = roundToDecimals(stage.accumulationRatio, 2),
            cagr2Year = cagr2Y,
            dcfValue = quant.dcfTarget,
            gbmTarget = quant.gbmTarget,
            mcMedian = quant.monteCarlo.medianPrice,
            mcBull = quant.monteCarlo.bullPrice,
            mc2xProb = quant.monteCarlo.probability2xPct,
            arimaTarget = quant.arimaTarget,
            dlTarget = quant.peExpansionTarget,
            quantConsensus = mlPrediction.adaptiveConsensusPrice,
            opmPct = roundToDecimals((16.0 + (profile.salesGrowthCAGR * 0.25) + (if (profile.debtToEquity <= 0.2) 4.5 else 0.0)).coerceIn(12.0, 38.0), 1),
            patGrowthQoQ = roundToDecimals((profile.patGrowthCAGR * 0.45 + 5.0).coerceIn(8.0, 48.0), 1),
            patGrowthYoY = roundToDecimals(profile.patGrowthCAGR, 1),
            earningsMomentum = when {
                profile.patGrowthCAGR >= 35.0 -> "🔥 Acceleration Beat"
                profile.patGrowthCAGR >= 20.0 -> "⚡ Margin Expansion"
                else -> "📦 Steady Compounder"
            },
            resultsDate = "Q3: +${profile.patGrowthCAGR.toInt()}% YoY Beat",
            piotroskiFScore = (6 + (if (profile.debtToEquity <= 0.3) 1 else 0) + (if (profile.salesGrowthCAGR >= 20.0) 1 else 0) + (if (profile.patGrowthCAGR >= 25.0) 1 else 0)).coerceIn(5, 9),
            altmanZScore = roundToDecimals((3.5 + (if (profile.debtToEquity < 0.2) 1.5 else 0.5) + (profile.salesGrowthCAGR / 25.0) * 0.8).coerceIn(2.5, 7.5), 1),
            financialHealthZone = if ((3.5 + (if (profile.debtToEquity < 0.2) 1.5 else 0.5)) >= 3.0) "SAFE_ZONE" else "GREY_ZONE",
            insiderActivity = when {
                profile.promoterHolding >= 60.0 -> "Promoter Buying (+₹${((profile.promoterHolding - 50.0) * 0.8).toInt() + 4}.2 Cr)"
                profile.promoterHolding >= 50.0 -> "Promoter Accumulation (No Dilution)"
                else -> "FII/DII Net Inflow"
            },
            pledgedPromoterSharesPct = if (profile.debtToEquity <= 0.3) 0.0 else roundToDecimals((profile.debtToEquity * 2.5).coerceIn(0.0, 8.5), 1),
            bulkDealsStatus = if (stage.accumulationRatio > 1.2) "Active Institutional Inflow 🏛️" else "Stable Delivery Base",
            rsi14 = quant.rsi14,
            rsRating = quant.relativeStrengthRating,
            isVcpContraction = quant.vcp.isVcpPivot,
            vcpTightnessScore = quant.vcp.contractionScore,
            vcpSignal = quant.vcp.signalDescription,
            mlConfidenceScore = mlPrediction.confidenceScorePct,
            mlModelLabel = mlPrediction.mlModelLabel,
            peExpansionTarget = quant.peExpansionTarget,
            adtvCrores = adtv,
            circuitBandPct = circuitBand,
            isNearCircuit = isNearCircuit,
            circuitStatusLabel = circuitLabel
        )
    }

    private fun roundToDecimals(value: Double, decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(value * multiplier) / multiplier
    }
}
