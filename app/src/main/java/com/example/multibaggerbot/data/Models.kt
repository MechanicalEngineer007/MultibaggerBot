package com.example.multibaggerbot.data

import kotlinx.serialization.Serializable

@Serializable
data class YahooChartResponse(
    val chart: Chart? = null
)

@Serializable
data class Chart(
    val result: List<ChartResult>? = null,
    val error: YahooError? = null
)

@Serializable
data class YahooError(
    val code: String,
    val description: String
)

@Serializable
data class ChartResult(
    val meta: Meta? = null,
    val timestamp: List<Long>? = null,
    val indicators: Indicators? = null
)

@Serializable
data class Meta(
    val symbol: String,
    val regularMarketPrice: Double? = null,
    val fiftyTwoWeekHigh: Double? = null,
    val fiftyTwoWeekLow: Double? = null,
    val regularMarketVolume: Long? = null,
    val longName: String? = null,
    val shortName: String? = null
)

@Serializable
data class Indicators(
    val quote: List<Quote>? = null
)

@Serializable
data class Quote(
    val open: List<Double?>? = null,
    val high: List<Double?>? = null,
    val low: List<Double?>? = null,
    val close: List<Double?>? = null,
    val volume: List<Long?>? = null
)

// ─── Domain Models ──────────────────────────────────────────────────────────

data class Candle(
    val date: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

data class StockData(
    val symbol: String,
    val candles: List<Candle>,
    val meta: Meta? = null
)

@Serializable
data class MultibaggerPick(
    val symbol: String,
    val companyName: String,
    val cmp: Double,
    val score: Int,                         // 0 - 100
    val convictionGrade: String,            // "AAA Prime", "AA Emerging", "A Compounder"
    val stageStatus: String,                // "Stage 2 Breakout", "Base Accumulation"
    val target1Year: Double,
    val target1YearPct: Double,             // Expected gain % (e.g. 35.0%)
    val target2Year: Double,
    val target2YearPct: Double,             // Expected gain % (e.g. 110.0%)
    val trailingStopLoss: Double,
    val riskRewardRatio: Double,
    val salesGrowthCAGR: Double,            // %
    val patGrowthCAGR: Double,              // %
    val debtToEquity: Double,
    val peRatio: Double,
    val pegRatio: Double,
    val promoterHolding: Double,            // %
    val institutionalHolding: Double,       // %
    val sectorTheme: String,
    val moatDescription: String,
    val sma50: Double = 0.0,
    val sma150: Double = 0.0,
    val sma200: Double = 0.0,
    val fiftyTwoWeekHigh: Double = 0.0,
    val fiftyTwoWeekLow: Double = 0.0,
    val volumeSurgeRatio: Double = 1.0,
    val cagr2Year: Double = 0.0,
    val dcfValue: Double = 0.0,
    val gbmTarget: Double = 0.0,
    val mcMedian: Double = 0.0,
    val mcBull: Double = 0.0,
    val mc2xProb: Double = 0.0,
    val arimaTarget: Double = 0.0,
    val dlTarget: Double = 0.0,
    val quantConsensus: Double = 0.0,
    val opmPct: Double = 22.5,
    val patGrowthQoQ: Double = 18.0,
    val patGrowthYoY: Double = 36.0,
    val earningsMomentum: String = "🔥 Acceleration Beat",
    val resultsDate: String = "Q3: +36% YoY Beat",
    val piotroskiFScore: Int = 8,
    val altmanZScore: Double = 4.5,
    val financialHealthZone: String = "SAFE_ZONE",
    val insiderActivity: String = "Promoter Buying +₹12.4 Cr",
    val pledgedPromoterSharesPct: Double = 0.0,
    val bulkDealsStatus: String = "Institutional Net Inflow",
    val rsi14: Double = 58.5,
    val rsRating: Int = 82,
    val isVcpContraction: Boolean = true,
    val vcpTightnessScore: Int = 88,
    val vcpSignal: String = "🔥 Tight VCP 3-Wave Pivot (<4.5% Risk)",
    val mlConfidenceScore: Int = 89,
    val mlModelLabel: String = "Adaptive ML Consensus (Top: Monte Carlo • 88.5% Acc)",
    val peExpansionTarget: Double = 0.0,
    val adtvCrores: Double = 35.4,
    val circuitBandPct: Double = 20.0,
    val isNearCircuit: Boolean = false,
    val circuitStatusLabel: String = "🟢 High Float (₹35.4 Cr/Day)"
)

@Serializable
data class ActiveAuditPosition(
    val ticker: String,
    val companyName: String,
    val sector: String,
    val entryPrice: Double,
    val currentPrice: Double,
    val pnlPct: Double,
    val target1Year2x: Double,
    val stopLoss: Double,
    val healthStatus: String = "HEALTHY_COMPOUNDER", // "HEALTHY_COMPOUNDER", "WATCH_CAUTION", "DUMP_WARNING"
    val dumpWarnings: List<String> = emptyList(),
    val lastAudited: String = "",
    val dcf1YrFV: Double = 0.0,
    val gbm1Yr: Double = 0.0,
    val mcMedian: Double = 0.0,
    val mcBull: Double = 0.0,
    val mc2xProb: Double = 0.0,
    val arima1Yr: Double = 0.0,
    val dl1Yr: Double = 0.0,
    val quantConsensus: Double = 0.0,
    val piotroskiFScore: Int = 8,
    val altmanZScore: Double = 4.2,
    val financialHealthZone: String = "SAFE_ZONE",
    val opmPct: Double = 22.0,
    val earningsBeat: String = "+34% YoY Beat",
    val insiderActivity: String = "Promoter Open-Market Buying",
    val adtvCrores: Double = 28.5,
    val circuitStatus: String = "🟢 High Liquidity",
    val notes: String = "Stage-2 breakout on accelerating profit growth",
    val holdingDays: Int = 180,
    val investedCapital: Double = 50000.0,
    val taxRatePct: Double = 20.0,
    val estimatedTax: Double = 0.0,
    val postTaxGain: Double = 0.0,
    val profitLadderMilestone: String = "📈 Riding Stage-2 Trend"
)

@Serializable
data class WatchlistStock(
    val symbol: String,
    val companyName: String,
    val addedPrice: Double,
    val addedDate: String,
    val target1Year: Double,
    val target2Year: Double,
    val score: Int,
    val notes: String = ""
)

@Serializable
data class TelegramConfig(
    val botToken: String = "",
    val chatId: String = "",
    val isEnabled: Boolean = false
)

@Serializable
data class NseSymbolItem(
    val symbol: String,
    val rawSymbol: String,
    val companyName: String,
    val series: String = "EQ"
)

@Serializable
data class SimulationStockItem(
    val symbol: String,
    val companyName: String,
    val initialPrice: Double,
    val currentPrice: Double,
    val shares: Int,
    val target1YearPrice: Double = 0.0,
    val target2YearPrice: Double = 0.0
)

@Serializable
data class FiiDiiData(
    val fiiNetCrores: Double = 1420.5,
    val diiNetCrores: Double = 2180.0,
    val netTotalCrores: Double = 3600.5,
    val sentiment: String = "🔥 Heavy Institutional Accumulation",
    val date: String = "Latest Trading Session"
)

@Serializable
data class FullAppBackup(
    val version: String = "1.0.0",
    val backupDate: String = "",
    val auditPositions: List<ActiveAuditPosition> = emptyList(),
    val simulationConfigs: List<SimulationConfig> = emptyList(),
    val watchlist: List<WatchlistStock> = emptyList(),
    val mlAccuracy: Double = 88.5,
    val mlCycles: Int = 14
)

@Serializable
data class SimulationConfig(
    val id: String,
    val name: String,
    val initialCapital: Double,
    val targetReturnPct: Double = 100.0,
    val timeHorizonMonths: Int = 12,
    val trailingStopLossPct: Double = 15.0,
    val createdDate: String = "",
    val stocks: List<SimulationStockItem> = emptyList(),
    val notes: String = ""
)

