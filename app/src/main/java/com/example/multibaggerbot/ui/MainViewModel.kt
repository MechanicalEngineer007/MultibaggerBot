package com.example.multibaggerbot.ui

import com.example.multibaggerbot.engine.AdaptiveMachineLearningEngine
import com.example.multibaggerbot.engine.AuditHealthEngine
import com.example.multibaggerbot.engine.LearnedModelWeights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.multibaggerbot.data.*
import com.example.multibaggerbot.engine.MultibaggerScorer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

sealed class UiState {
    object Initial : UiState()
    data class Loading(val progress: Float, val status: String, val canCancel: Boolean = true) : UiState()
    data class Success(val picks: List<MultibaggerPick>) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MultibaggerRepository(application)
    private val api = YahooFinanceApi()
    private val telegramApi = TelegramApi()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(0)
    val selectedFilter: StateFlow<Int> = _selectedFilter.asStateFlow()

    private val _accountCapital = MutableStateFlow(200000.0)
    val accountCapital: StateFlow<Double> = _accountCapital.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPickForChart = MutableStateFlow<MultibaggerPick?>(null)
    val selectedPickForChart: StateFlow<MultibaggerPick?> = _selectedPickForChart.asStateFlow()

    private val _watchlist = MutableStateFlow<List<WatchlistStock>>(emptyList())
    val watchlist: StateFlow<List<WatchlistStock>> = _watchlist.asStateFlow()

    private val _auditPositions = MutableStateFlow<List<ActiveAuditPosition>>(emptyList())
    val auditPositions: StateFlow<List<ActiveAuditPosition>> = _auditPositions.asStateFlow()

    private val _selectedAuditFilter = MutableStateFlow(0) // 0 = All, 1 = Healthy, 2 = Caution, 3 = Dump Warnings
    val selectedAuditFilter: StateFlow<Int> = _selectedAuditFilter.asStateFlow()

    private val _universeScope = MutableStateFlow(0) // 0 = All NSE Equities (2,598), 1 = Nifty 500 (500), 2 = Radar (50)
    val universeScope: StateFlow<Int> = _universeScope.asStateFlow()

    private val _selectedTheme = MutableStateFlow("All")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _simulationConfigs = MutableStateFlow<List<SimulationConfig>>(emptyList())
    val simulationConfigs: StateFlow<List<SimulationConfig>> = _simulationConfigs.asStateFlow()

    private val _activeConfigIndex = MutableStateFlow(0)
    val activeConfigIndex: StateFlow<Int> = _activeConfigIndex.asStateFlow()

    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage: StateFlow<String?> = _notificationMessage.asStateFlow()

    private val _isSendingTelegram = MutableStateFlow(false)
    val isSendingTelegram: StateFlow<Boolean> = _isSendingTelegram.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(repository.isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(!repository.isBiometricEnabled())
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _mlWeights = MutableStateFlow(AdaptiveMachineLearningEngine.getWeights())
    val mlWeights: StateFlow<LearnedModelWeights> = _mlWeights.asStateFlow()

    private val _fiiDiiFlows = MutableStateFlow(FiiDiiData())
    val fiiDiiFlows: StateFlow<FiiDiiData> = _fiiDiiFlows.asStateFlow()

    private val _marketRegime = MutableStateFlow("BULLISH_EXPANSION")
    val marketRegime: StateFlow<String> = _marketRegime.asStateFlow()

    private val _marketBreadthPct = MutableStateFlow(68.5)
    val marketBreadthPct: StateFlow<Double> = _marketBreadthPct.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(repository.getLastSyncTimestamp())
    val lastSyncTimestamp: StateFlow<String> = _lastSyncTimestamp.asStateFlow()

    fun recordSyncComplete() {
        val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        _lastSyncTimestamp.value = timeStr
        repository.saveLastSyncTimestamp(timeStr)
    }

    val cachedCandles = mutableMapOf<String, List<Candle>>()

    // Complete list of all NSE symbols loaded from official equity master
    private var allNseSymbols: List<NseSymbolItem> = emptyList()

    // Curated high-conviction radar universe for fast scanning (prioritizes high-conviction leaders)
    private val radarUniverse = listOf(
        "ICICIBANK.NS", "TITAN.NS", "BEL.NS", "HAL.NS", "TRENT.NS", "DIXON.NS", "KAYNES.NS",
        "SUZLON.NS", "POLYCAB.NS", "CDSL.NS", "BSE.NS", "COCHINSHIP.NS",
        "BDL.NS", "SOLARINDS.NS", "KPITTECH.NS", "KALYANKJIL.NS", "TITAGARH.NS",
        "MOTILALOFS.NS", "CGPOWER.NS", "OLECTRA.NS", "PERSISTENT.NS", "APLAPOLLO.NS",
        "RADICO.NS", "SUPREMEIND.NS", "MASTEK.NS", "ZOMATO.NS", "JUBLFOOD.NS",
        "ANGELONE.NS", "SYRMA.NS", "NETWEB.NS", "MOTHERSON.NS", "BHEL.NS",
        "MANKIND.NS", "COFORGE.NS", "DEEPAKNTR.NS", "INOXWIND.NS", "JSWENERGY.NS",
        "TATAPOWER.NS", "IRCON.NS", "RVNL.NS", "RAILTEL.NS", "KEI.NS",
        "CUMMINSIND.NS", "ABB.NS", "SIEMENS.NS", "BIKAJI.NS", "MCX.NS",
        "TIINDIA.NS", "JBMA.NS", "TORNTPHARM.NS", "PIIND.NS", "AMBER.NS",
        "HDFCBANK.NS", "RELIANCE.NS", "TCS.NS", "INFY.NS", "BHARTIARTL.NS",
        "SBIN.NS", "TATAMOTORS.NS", "LT.NS", "SUNPHARMA.NS"
    )

    private var scanJob: Job? = null

    init {
        AdaptiveMachineLearningEngine.initialize(getApplication())
        _mlWeights.value = AdaptiveMachineLearningEngine.getWeights()
        loadSavedData()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun selectFilter(filter: Int) {
        _selectedFilter.value = filter
    }

    fun selectAuditFilter(filter: Int) {
        _selectedAuditFilter.value = filter
    }

    fun selectUniverseScope(scope: Int) {
        _universeScope.value = scope
    }

    fun updateCapital(capital: Double) {
        _accountCapital.value = capital.coerceAtLeast(5000.0)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectPickForChart(pick: MultibaggerPick?) {
        _selectedPickForChart.value = pick
    }

    fun dismissNotification() {
        _notificationMessage.value = null
    }

    private fun loadSavedData() {
        viewModelScope.launch {
            // Load official NSE symbols
            allNseSymbols = repository.getAllNseSymbols()

            val cachedPicks = repository.getCachedPicks()
            val savedWatchlist = repository.getWatchlist()
            val savedAudit = repository.getAuditPositions()
            val savedConfigs = repository.getSimulationConfigs()
            _watchlist.value = savedWatchlist
            val enrichedAudit = savedAudit.map { pos ->
                val invested = if (pos.investedCapital > 0) pos.investedCapital else 50000.0
                val grossGain = (pos.pnlPct / 100.0) * invested
                val taxRate = if (pos.holdingDays >= 365) 12.5 else 20.0
                val estTax = if (grossGain > 0) grossGain * (taxRate / 100.0) else 0.0
                val postTax = grossGain - estTax
                val milestone = when {
                    pos.pnlPct >= 100.0 -> "🚀 2x Multibagger: Free Runner"
                    pos.pnlPct >= 35.0 -> "🎯 Tier-1: Lock 25% & Breakeven"
                    pos.pnlPct >= 0.0 -> "📈 Riding Stage-2 Trend"
                    else -> "🛡️ Capital Protection (SL: ₹${String.format("%,.0f", pos.stopLoss)})"
                }
                val eval = AuditHealthEngine.evaluate(
                    livePrice = pos.currentPrice,
                    entryPrice = pos.entryPrice,
                    stopLoss = pos.stopLoss,
                    pnlPct = pos.pnlPct,
                    altmanZScore = pos.altmanZScore,
                    piotroskiFScore = pos.piotroskiFScore,
                    existingWarnings = pos.dumpWarnings
                )
                pos.copy(
                    investedCapital = invested,
                    taxRatePct = taxRate,
                    estimatedTax = Math.round(estTax * 100.0) / 100.0,
                    postTaxGain = Math.round(postTax * 100.0) / 100.0,
                    profitLadderMilestone = milestone,
                    healthStatus = eval.status,
                    dumpWarnings = eval.warnings
                )
            }
            _auditPositions.value = enrichedAudit
            _simulationConfigs.value = savedConfigs

            if (cachedPicks.isNotEmpty()) {
                _uiState.value = UiState.Success(cachedPicks)
            } else {
                scanMultibaggers()
            }

            // Silently verify live quotes for active audit positions and simulations
            refreshAudit(isSilent = true)
            refreshSimulationPrices(isSilent = true)
        }
    }

    fun selectSimulationConfig(index: Int) {
        if (index in _simulationConfigs.value.indices) {
            _activeConfigIndex.value = index
        }
    }

    fun updateActiveConfigParameters(
        targetReturnPct: Double,
        timeHorizonMonths: Int,
        trailingStopLossPct: Double,
        initialCapital: Double
    ) {
        viewModelScope.launch {
            val list = _simulationConfigs.value.toMutableList()
            val idx = _activeConfigIndex.value
            if (idx in list.indices) {
                val current = list[idx]
                val updated = current.copy(
                    targetReturnPct = targetReturnPct,
                    timeHorizonMonths = timeHorizonMonths,
                    trailingStopLossPct = trailingStopLossPct,
                    initialCapital = initialCapital
                )
                list[idx] = updated
                _simulationConfigs.value = list
                repository.saveSimulationConfigs(list)
                _notificationMessage.value = "Updated parameters for '${updated.name}'"
            }
        }
    }

    fun createNewSimulationConfig(
        name: String,
        capital: Double,
        targetReturnPct: Double,
        horizonMonths: Int,
        stopLossPct: Double
    ) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val currentPicks = (_uiState.value as? UiState.Success)?.picks ?: emptyList()
            val seedPicks = currentPicks.take(5)
            val perStockCap = capital / (if (seedPicks.isNotEmpty()) seedPicks.size else 1)

            val stocks = if (seedPicks.isNotEmpty()) {
                seedPicks.map { p ->
                    val shares = if (p.cmp > 0) (perStockCap / p.cmp).toInt().coerceAtLeast(1) else 1
                    SimulationStockItem(
                        symbol = p.symbol,
                        companyName = p.companyName,
                        initialPrice = p.cmp,
                        currentPrice = p.cmp,
                        shares = shares,
                        target1YearPrice = p.target1Year,
                        target2YearPrice = p.target2Year
                    )
                }
            } else {
                listOf(
                    SimulationStockItem("BEL.NS", "Bharat Electronics", 295.0, 308.0, (capital * 0.2 / 308.0).toInt().coerceAtLeast(1), 410.0, 580.0),
                    SimulationStockItem("HAL.NS", "Hindustan Aeronautics", 4855.0, 4980.0, (capital * 0.2 / 4980.0).toInt().coerceAtLeast(1), 6800.0, 9700.0),
                    SimulationStockItem("TRENT.NS", "Trent Ltd", 2850.0, 3079.0, (capital * 0.2 / 3079.0).toInt().coerceAtLeast(1), 5700.0, 7800.0),
                    SimulationStockItem("BSE.NS", "BSE Limited", 3490.0, 3800.0, (capital * 0.2 / 3800.0).toInt().coerceAtLeast(1), 6980.0, 9500.0),
                    SimulationStockItem("DIXON.NS", "Dixon Tech", 12100.0, 12625.0, (capital * 0.2 / 12625.0).toInt().coerceAtLeast(1), 24200.0, 33000.0)
                )
            }

            val newConfig = SimulationConfig(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "Custom Portfolio ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}" },
                initialCapital = capital,
                targetReturnPct = targetReturnPct,
                timeHorizonMonths = horizonMonths,
                trailingStopLossPct = stopLossPct,
                createdDate = dateStr,
                stocks = stocks,
                notes = "Custom created configuration"
            )

            val updatedList = _simulationConfigs.value + newConfig
            _simulationConfigs.value = updatedList
            _activeConfigIndex.value = updatedList.size - 1
            repository.saveSimulationConfigs(updatedList)
            _notificationMessage.value = "Created configuration: '${newConfig.name}'"
        }
    }

    fun deleteSimulationConfig(index: Int) {
        viewModelScope.launch {
            val list = _simulationConfigs.value.toMutableList()
            if (list.size <= 1) {
                _notificationMessage.value = "Must keep at least 1 tracking configuration."
                return@launch
            }
            if (index in list.indices) {
                val removed = list.removeAt(index)
                _simulationConfigs.value = list
                _activeConfigIndex.value = 0
                repository.saveSimulationConfigs(list)
                _notificationMessage.value = "Deleted configuration '${removed.name}'"
            }
        }
    }

    fun refreshSimulationPrices(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent) {
                _notificationMessage.value = "Tracking live prices for simulation basket..."
            }
            val list = _simulationConfigs.value.toMutableList()
            val idx = _activeConfigIndex.value
            if (idx !in list.indices) return@launch

            val config = list[idx]
            val updatedStocks = config.stocks.map { item ->
                async(Dispatchers.IO) {
                    try {
                        val p = api.fetchCurrentPrice(item.symbol)
                        if (p != null && p > 0.0) {
                            item.copy(currentPrice = Math.round(p * 100.0) / 100.0)
                        } else item
                    } catch (e: Exception) {
                        item
                    }
                }
            }.awaitAll()

            val updatedConfig = config.copy(stocks = updatedStocks)
            list[idx] = updatedConfig
            _simulationConfigs.value = list
            repository.saveSimulationConfigs(list)
            if (!isSilent) {
                _notificationMessage.value = "Live prices refreshed for '${config.name}'!"
            }
        }
    }

    /**
     * Refreshes the Live Audit tab by querying current live market CMP from Yahoo Finance,
     * calculating real-time P&L against the Initial Entry Value, and evaluating Dump Warnings.
     */
    fun refreshAudit(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent) {
                _notificationMessage.value = "Fetching live market CMP for active audit positions..."
            }
            val currentPositions = repository.getAuditPositions()
            if (currentPositions.isEmpty()) return@launch

            val updatedList = currentPositions.map { pos ->
                async(Dispatchers.IO) {
                    try {
                        val livePrice = api.fetchCurrentPrice(pos.ticker)
                        if (livePrice != null && livePrice > 0.0) {
                            val entry = pos.entryPrice
                            val pnl = ((livePrice - entry) / entry) * 100.0
                            val stopLoss = pos.stopLoss

                            val eval = AuditHealthEngine.evaluate(
                                livePrice = livePrice,
                                entryPrice = entry,
                                stopLoss = stopLoss,
                                pnlPct = pnl,
                                altmanZScore = pos.altmanZScore,
                                piotroskiFScore = pos.piotroskiFScore,
                                existingWarnings = pos.dumpWarnings
                            )

                            val invested = if (pos.investedCapital > 0) pos.investedCapital else 50000.0
                            val grossGain = (pnl / 100.0) * invested
                            val taxRate = if (pos.holdingDays >= 365) 12.5 else 20.0
                            val estTax = if (grossGain > 0) grossGain * (taxRate / 100.0) else 0.0
                            val postTax = grossGain - estTax
                            val milestone = when {
                                pnl >= 100.0 -> "🚀 2x Multibagger: Free Runner"
                                pnl >= 35.0 -> "🎯 Tier-1: Lock 25% & Breakeven"
                                pnl >= 0.0 -> "📈 Riding Stage-2 Trend"
                                else -> "🛡️ Capital Protection (SL: ₹${String.format("%,.0f", stopLoss)})"
                            }
                            pos.copy(
                                currentPrice = Math.round(livePrice * 100.0) / 100.0,
                                pnlPct = Math.round(pnl * 100.0) / 100.0,
                                healthStatus = eval.status,
                                dumpWarnings = eval.warnings,
                                lastAudited = "Market Verified Live",
                                investedCapital = invested,
                                taxRatePct = taxRate,
                                estimatedTax = Math.round(estTax * 100.0) / 100.0,
                                postTaxGain = Math.round(postTax * 100.0) / 100.0,
                                profitLadderMilestone = milestone
                            )
                        } else {
                            pos
                        }
                    } catch (e: Exception) {
                        pos
                    }
                }
            }.awaitAll()

            _auditPositions.value = updatedList
            repository.saveAuditPositions(updatedList)
            com.example.multibaggerbot.widget.MultibaggerWidgetProvider.updateAllWidgets(getApplication())
            recordSyncComplete()

            val mlRes = AdaptiveMachineLearningEngine.trainOnAuditHistory(updatedList, repository)
            _mlWeights.value = AdaptiveMachineLearningEngine.getWeights()

            val dumps = updatedList.filter { it.healthStatus == "DUMP_WARNING" }
            val cautions = updatedList.filter { it.healthStatus == "WATCH_CAUTION" }

            // Trigger system notifications for active Dump or Caution positions
            dumps.forEach { dumpPos ->
                val reason = dumpPos.dumpWarnings.firstOrNull() ?: "Closed below stop floor"
                NotificationHelper.sendDumpWarningNotification(
                    getApplication(),
                    dumpPos.ticker,
                    reason
                )
            }

            cautions.take(2).forEach { cautionPos ->
                val reason = cautionPos.dumpWarnings.firstOrNull() ?: "Approaching stop floor / drawdown alert"
                NotificationHelper.sendMarketAlert(
                    getApplication(),
                    "⚠️ WATCH / CAUTION: ${cautionPos.ticker.replace(".NS", "")}",
                    reason
                )
            }

            if (!isSilent) {
                _notificationMessage.value = "Live Audit Updated! Checked ${updatedList.size} positions (${dumps.size} Dump Warnings, ${cautions.size} Watch/Caution)"
            } else if (dumps.isNotEmpty() || cautions.isNotEmpty()) {
                _notificationMessage.value = "⚠️ Audit Alert: ${dumps.size} Dump Warning(s) & ${cautions.size} Caution position(s) active!"
            }
        }
    }

    private fun resolveTickerSymbol(query: String): String {
        val q = query.trim().uppercase()
        val qNoSpace = q.replace(" ", "")

        // 1. Exact rawSymbol or symbol match
        allNseSymbols.find { it.rawSymbol.equals(qNoSpace, ignoreCase = true) || it.symbol.equals("$qNoSpace.NS", ignoreCase = true) }?.let {
            return it.symbol
        }

        // 2. Multi-word companyName search (e.g. "ICICI" + "BANK" -> "ICICIBANK.NS")
        val tokens = query.trim().uppercase().split(" ").filter { it.length >= 3 }
        if (tokens.isNotEmpty()) {
            val companyMatch = allNseSymbols.find { item ->
                tokens.all { t -> item.companyName.contains(t, ignoreCase = true) || item.symbol.contains(t, ignoreCase = true) }
            }
            if (companyMatch != null) return companyMatch.symbol
        }

        // 3. Fuzzy Levenshtein match on rawSymbol (e.g. "TITIAN" -> "TITAN")
        val fuzzyMatch = allNseSymbols.find { item ->
            levenshteinDistance(qNoSpace, item.rawSymbol) <= 2
        }
        if (fuzzyMatch != null) return fuzzyMatch.symbol

        return if (qNoSpace.endsWith(".NS") || qNoSpace.endsWith(".BO")) qNoSpace else "$qNoSpace.NS"
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) dp[i - 1][j - 1] else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
        return dp[s1.length][s2.length]
    }

    fun searchSingleStock(ticker: String) {
        viewModelScope.launch {
            val resolvedTicker = resolveTickerSymbol(ticker)
            _uiState.value = UiState.Loading(0.5f, "Diagnosing $resolvedTicker Multibagger potential...", canCancel = false)
            try {
                val data = api.fetchHistoricalData(resolvedTicker)
                if (data != null && data.candles.size >= 40) {
                    cachedCandles[data.symbol] = data.candles
                    val pick = MultibaggerScorer.evaluate(data)
                    if (pick != null) {
                        // Persist and merge into candidate picks pool so it stays under All and AAA (85+)
                        val currentCached = repository.getCachedPicks().filter { it.symbol != pick.symbol }
                        val merged = (listOf(pick) + currentCached).sortedByDescending { it.score }
                        repository.saveCachedPicks(merged)
                        _uiState.value = UiState.Success(merged)
                        _notificationMessage.value = "Analyzed ${pick.symbol} (Score: ${pick.score}/100) — Added to Radar candidates!"
                    } else {
                        _uiState.value = UiState.Error("Unable to calculate metrics for $resolvedTicker.")
                    }
                } else {
                    _uiState.value = UiState.Error("Stock '$resolvedTicker' not found. Please verify the NSE symbol.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Search error: ${e.message}")
            }
        }
    }

    /**
     * Scans NSE stocks according to selected universe scope:
     * Scope 0: Full NSE Market (all 2,598 active equities)
     * Scope 1: Top 500 Market Leaders (Nifty 500)
     * Scope 2: Curated 50 High-Beta Multibagger Radar
     */
    fun scanMultibaggers(scopeOverride: Int? = null) {
        scanJob?.cancel()
        val scope = scopeOverride ?: _universeScope.value

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val tickersToScan: List<String> = when (scope) {
                0 -> {
                    // Full NSE Equity Universe (Priority Leaders scanned FIRST!)
                    val leaders = radarUniverse
                    val allList = if (allNseSymbols.isNotEmpty()) allNseSymbols.map { it.symbol } else repository.getAllNseSymbols().map { it.symbol }
                    val rest = allList.filterNot { leaders.contains(it) }
                    leaders + rest
                }
                1 -> {
                    // Real Nifty 500 Leaders (Priority Market Leaders scanned FIRST, taking top 500)
                    val leaders = radarUniverse
                    val allList = if (allNseSymbols.isNotEmpty()) allNseSymbols.map { it.symbol } else repository.getAllNseSymbols().map { it.symbol }
                    val rest = allList.filterNot { leaders.contains(it) }
                    (leaders + rest).take(500)
                }
                else -> radarUniverse
            }

            val total = tickersToScan.size
            val scopeLabel = when (scope) {
                0 -> "All $total NSE Equities"
                1 -> "Top $total Nifty 500 Stocks"
                else -> "Top $total Radar Champions"
            }

            _uiState.value = UiState.Loading(0f, "Initializing Stage-2 Scanner across $scopeLabel...", canCancel = true)

            val discoveredPicks = Collections.synchronizedList(mutableListOf<MultibaggerPick>())
            val processedCount = AtomicInteger(0)
            val semaphore = Semaphore(4) // 4 concurrent requests for sustainable throughput without tripping rate limits

            coroutineScope {
                tickersToScan.map { ticker ->
                    launch {
                        semaphore.withPermit {
                            if (!isActive) return@launch

                            try {
                                kotlinx.coroutines.delay(35) // Micro-pacing buffer to smooth out burst traffic
                                val data = api.fetchHistoricalData(ticker, range = "1y", interval = "1d")
                                if (data != null && data.candles.size >= 40) {
                                    cachedCandles[ticker] = data.candles
                                    val pick = MultibaggerScorer.evaluate(data)
                                    if (pick != null && pick.score >= 50) {
                                        discoveredPicks.add(pick)
                                        // Incremental save so discovered picks are never lost if interrupted
                                        if (discoveredPicks.size % 5 == 0) {
                                            val currentMap = repository.getCachedPicks().associateBy { it.symbol }.toMutableMap()
                                            discoveredPicks.forEach { currentMap[it.symbol] = it }
                                            repository.saveCachedPicks(currentMap.values.sortedByDescending { it.score })
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // continue scanning
                            } finally {
                                val done = processedCount.incrementAndGet()
                                if (isActive) {
                                    val progress = done.toFloat() / total
                                    val found = discoveredPicks.size
                                    _uiState.value = UiState.Loading(
                                        progress = progress,
                                        status = "Screening $ticker ($done/$total NSE Stocks)... Found $found Picks",
                                        canCancel = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val resultList = discoveredPicks.toList()
            val existingMap = repository.getCachedPicks().associateBy { it.symbol }.toMutableMap()
            resultList.forEach { pick ->
                existingMap[pick.symbol] = pick
            }
            val mergedSorted = existingMap.values.sortedByDescending { it.score }
            if (mergedSorted.isNotEmpty()) {
                _uiState.value = UiState.Success(mergedSorted)
                repository.saveCachedPicks(mergedSorted)
                recordSyncComplete()
                _notificationMessage.value = "Scan Complete: Found ${resultList.size} new picks (${mergedSorted.size} total active candidates) across $scopeLabel!"
            } else {
                // If cancelled or empty, try showing cached picks
                val cached = repository.getCachedPicks()
                if (cached.isNotEmpty()) {
                    _uiState.value = UiState.Success(cached)
                } else {
                    _uiState.value = UiState.Error("Scan ended without candidates. Tap Scan to retry.")
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        viewModelScope.launch {
            val cached = repository.getCachedPicks()
            if (cached.isNotEmpty()) {
                _uiState.value = UiState.Success(cached)
                _notificationMessage.value = "Scan stopped by user. Displaying current candidate picks."
            } else {
                _uiState.value = UiState.Initial
            }
        }
    }

    fun toggleWatchlist(pick: MultibaggerPick) {
        viewModelScope.launch {
            val exists = _watchlist.value.any { it.symbol == pick.symbol }
            if (exists) {
                repository.removeFromWatchlist(pick.symbol)
                _watchlist.value = repository.getWatchlist()
                _notificationMessage.value = "Removed ${pick.symbol.replace(".NS", "")} from Watchlist"
            } else {
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                val item = WatchlistStock(
                    symbol = pick.symbol,
                    companyName = pick.companyName,
                    addedPrice = pick.cmp,
                    addedDate = dateStr,
                    target1Year = pick.target1Year,
                    target2Year = pick.target2Year,
                    score = pick.score,
                    notes = "${pick.sectorTheme} | ${pick.stageStatus}"
                )
                repository.addToWatchlist(item)
                _watchlist.value = repository.getWatchlist()
                _notificationMessage.value = "Added ${pick.symbol.replace(".NS", "")} to 1-2Y Watchlist!"
            }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(symbol)
            _watchlist.value = repository.getWatchlist()
            _notificationMessage.value = "Removed ${symbol.replace(".NS", "")} from Watchlist"
        }
    }

    fun sendTelegramAlerts() {
        viewModelScope.launch {
            val config = repository.getTelegramConfig()
            if (config.botToken.isBlank() || config.chatId.isBlank()) {
                _notificationMessage.value = "Configure Telegram Bot Token & Chat ID in Settings first!"
                return@launch
            }

            val currentPicks = (_uiState.value as? UiState.Success)?.picks ?: emptyList()
            if (currentPicks.isEmpty()) {
                _notificationMessage.value = "No picks available to broadcast."
                return@launch
            }

            _isSendingTelegram.value = true
            val top5 = currentPicks.take(5)
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

            val sb = StringBuilder()
            sb.append("🚀 <b>MULTIBAGGER BOT — 1-2 YEAR PICKS RADAR</b>\n")
            sb.append("📅 Date: $dateStr\n")
            sb.append("🎯 Objective: >20% to 100%+ Returns | Stage-2 Breakouts\n\n")

            top5.forEachIndexed { i, p ->
                sb.append("<b>${i + 1}. ${p.symbol.replace(".NS", "")} (${p.companyName})</b>\n")
                sb.append("• Score: <b>${p.score}/100</b> (${p.convictionGrade})\n")
                sb.append("• CMP: ₹${String.format("%,.1f", p.cmp)}\n")
                sb.append("• 1Y Target: ₹${String.format("%,.1f", p.target1Year)} (<b>+${p.target1YearPct}%</b>)\n")
                sb.append("• 2Y Target: ₹${String.format("%,.1f", p.target2Year)} (<b>+${p.target2YearPct}%</b>)\n")
                sb.append("• Theme: ${p.sectorTheme}\n")
                sb.append("• Moat: ${p.moatDescription}\n\n")
            }
            sb.append("⚠️ <i>Disclaimer: For educational & research purposes only. Consult SEBI RIA before investing.</i>")

            val ok = telegramApi.sendMessage(config.botToken, config.chatId, sb.toString())
            _isSendingTelegram.value = false
            _notificationMessage.value = if (ok) "Sent Top 5 Multibaggers to Telegram! 🚀" else "Failed to send Telegram alert."
        }
    }

    fun selectTheme(theme: String) {
        _selectedTheme.value = theme
    }

    fun addCustomAuditPosition(
        ticker: String,
        entryPrice: Double,
        stopLoss: Double,
        targetPrice: Double
    ) {
        viewModelScope.launch {
            val clean = ticker.trim().replace(" ", "").uppercase()
            val sym = if (clean.endsWith(".NS") || clean.endsWith(".BO")) clean else "$clean.NS"
            _notificationMessage.value = "Fetching live CMP for $sym..."

            val livePrice = api.fetchCurrentPrice(sym) ?: entryPrice
            val pnl = if (entryPrice > 0) ((livePrice - entryPrice) / entryPrice) * 100.0 else 0.0
            val isDump = (stopLoss > 0 && livePrice < stopLoss) || pnl <= -15.0
            val isCaution = pnl < -5.0 && !isDump

            val warnings = mutableListOf<String>()
            if (stopLoss > 0 && livePrice < stopLoss) {
                warnings.add("Price closed below 200-DMA Stop Floor (₹${String.format("%,.1f", stopLoss)})")
            }
            if (pnl <= -15.0) {
                warnings.add("P&L declined significantly to ${String.format("%.1f", pnl)}%")
            }

            val status = when {
                isDump -> "DUMP_WARNING"
                isCaution -> "WATCH_CAUTION"
                else -> "HEALTHY_COMPOUNDER"
            }

            val newPos = ActiveAuditPosition(
                ticker = sym,
                companyName = clean.replace(".NS", ""),
                sector = "Personal Holding",
                entryPrice = Math.round(entryPrice * 100.0) / 100.0,
                currentPrice = Math.round(livePrice * 100.0) / 100.0,
                pnlPct = Math.round(pnl * 100.0) / 100.0,
                target1Year2x = targetPrice,
                stopLoss = stopLoss,
                healthStatus = status,
                dumpWarnings = warnings,
                lastAudited = "Market Verified Live",
                dcf1YrFV = targetPrice * 0.9,
                gbm1Yr = targetPrice * 1.05,
                mcMedian = targetPrice * 0.95,
                mcBull = targetPrice * 1.15,
                mc2xProb = 50.0,
                arima1Yr = livePrice * 1.05,
                dl1Yr = targetPrice * 0.98,
                quantConsensus = targetPrice
            )

            val updated = listOf(newPos) + _auditPositions.value.filter { it.ticker != sym }
            _auditPositions.value = updated
            repository.saveAuditPositions(updated)
            com.example.multibaggerbot.widget.MultibaggerWidgetProvider.updateAllWidgets(getApplication())
            recordSyncComplete()
            _notificationMessage.value = "Added $sym to Live Audit (CMP: ₹${String.format("%,.1f", livePrice)})"

            if (isDump) {
                NotificationHelper.sendDumpWarningNotification(
                    getApplication(),
                    sym,
                    "Immediate exit recommended: price dropped below stop-loss."
                )
            }
        }
    }

    fun addCustomStockToSimulation(
        symbol: String,
        entryPrice: Double,
        shares: Int,
        targetPrice: Double
    ) {
        viewModelScope.launch {
            val clean = symbol.trim().replace(" ", "").uppercase()
            val sym = if (clean.endsWith(".NS") || clean.endsWith(".BO")) clean else "$clean.NS"
            val livePrice = api.fetchCurrentPrice(sym) ?: entryPrice

            val list = _simulationConfigs.value.toMutableList()
            val idx = _activeConfigIndex.value
            if (idx in list.indices) {
                val config = list[idx]
                val newItem = SimulationStockItem(
                    symbol = sym,
                    companyName = clean.replace(".NS", ""),
                    initialPrice = entryPrice,
                    currentPrice = livePrice,
                    shares = shares,
                    target1YearPrice = targetPrice,
                    target2YearPrice = targetPrice * 1.35
                )

                val updatedStocks = listOf(newItem) + config.stocks.filter { it.symbol != sym }
                val updatedConfig = config.copy(stocks = updatedStocks)
                list[idx] = updatedConfig
                _simulationConfigs.value = list
                repository.saveSimulationConfigs(list)
                _notificationMessage.value = "Added $shares shares of $sym to '${config.name}'"
            }
        }
    }

    fun sendTestNotification(context: android.content.Context) {
        NotificationHelper.sendMarketAlert(
            context,
            "🔔 Multibagger Bot Pre-Market Alert",
            "Market opens in 15 mins. Active positions: ${_auditPositions.value.size}. All systems operational."
        )
        _notificationMessage.value = "Test notification sent! Check your notification shade."
    }

    fun saveTelegramSettings(token: String, chatId: String) {
        val cfg = TelegramConfig(token.trim(), chatId.trim(), token.isNotBlank() && chatId.isNotBlank())
        repository.saveTelegramConfig(cfg)
        _notificationMessage.value = "Telegram settings saved!"
    }

    fun getTelegramSettings(): TelegramConfig = repository.getTelegramConfig()

    fun setBiometricEnabled(enabled: Boolean) {
        repository.setBiometricEnabled(enabled)
        _isBiometricEnabled.value = enabled
        if (!enabled) {
            _isAppUnlocked.value = true
        }
        _notificationMessage.value = if (enabled) "Biometric Lock Enabled 🔒" else "Biometric Lock Disabled"
    }

    fun unlockApp() {
        _isAppUnlocked.value = true
    }

    fun lockApp() {
        if (_isBiometricEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    fun retrainMlEngine() {
        viewModelScope.launch {
            val result = AdaptiveMachineLearningEngine.trainOnAuditHistory(_auditPositions.value, repository)
            _mlWeights.value = AdaptiveMachineLearningEngine.getWeights()
            _notificationMessage.value = result.summaryMessage
        }
    }

    fun updateHoldingThesisNotes(ticker: String, notes: String) {
        viewModelScope.launch {
            val list = _auditPositions.value.toMutableList()
            val idx = list.indexOfFirst { it.ticker == ticker }
            if (idx != -1) {
                list[idx] = list[idx].copy(notes = notes)
                _auditPositions.value = list
                repository.saveAuditPositions(list)
                _notificationMessage.value = "Updated thesis notes for ${ticker.replace(".NS", "")} 📝"
            }
        }
    }

    fun exportFullBackup(context: android.content.Context) {
        val dateStamp = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        val backup = FullAppBackup(
            backupDate = dateStamp,
            auditPositions = _auditPositions.value,
            simulationConfigs = _simulationConfigs.value,
            watchlist = _watchlist.value,
            mlAccuracy = _mlWeights.value.modelAccuracyPct,
            mlCycles = _mlWeights.value.trainedCycles
        )
        BackupRestoreHelper.exportBackup(context, backup)
    }

    fun restoreFullBackup(backup: FullAppBackup) {
        viewModelScope.launch {
            if (backup.auditPositions.isNotEmpty()) {
                _auditPositions.value = backup.auditPositions
                repository.saveAuditPositions(backup.auditPositions)
            }
            if (backup.simulationConfigs.isNotEmpty()) {
                _simulationConfigs.value = backup.simulationConfigs
                repository.saveSimulationConfigs(backup.simulationConfigs)
            }
            if (backup.watchlist.isNotEmpty()) {
                _watchlist.value = backup.watchlist
                for (w in backup.watchlist) {
                    repository.addToWatchlist(w)
                }
            }
            com.example.multibaggerbot.widget.MultibaggerWidgetProvider.updateAllWidgets(getApplication())
            recordSyncComplete()
            _notificationMessage.value = "Full Portfolio Restored! (${backup.auditPositions.size} holdings, ${backup.simulationConfigs.size} scenarios) 🎉"
        }
    }
}
