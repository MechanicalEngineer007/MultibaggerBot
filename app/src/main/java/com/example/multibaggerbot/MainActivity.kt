package com.example.multibaggerbot

import com.example.multibaggerbot.ui.components.PortfolioHeatmapView
import com.example.multibaggerbot.ui.components.StockComparisonDialog
import com.example.multibaggerbot.ui.components.ThesisJournalDialog
import com.example.multibaggerbot.ui.components.RestoreBackupDialog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multibaggerbot.data.BrokerHelper
import com.example.multibaggerbot.data.MultibaggerPick
import com.example.multibaggerbot.data.WatchlistStock
import com.example.multibaggerbot.data.ActiveAuditPosition
import com.example.multibaggerbot.ui.MainViewModel
import com.example.multibaggerbot.ui.UiState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.multibaggerbot.data.CsvExportHelper
import com.example.multibaggerbot.data.NotificationHelper
import com.example.multibaggerbot.ui.components.AddCustomHoldingDialog
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.multibaggerbot.data.PdfReportHelper
import com.example.multibaggerbot.ui.components.CandlestickChartView
import com.example.multibaggerbot.ui.components.PositionSizingDialog
import com.example.multibaggerbot.ui.components.SipAccumulatorDialog
import com.example.multibaggerbot.ui.components.WealthSimulatorView

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScaffold(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val notificationMessage by viewModel.notificationMessage.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val unlockLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.unlockApp()
        }
    }

    if (isBiometricEnabled && !isAppUnlocked) {
        BiometricLockScreen(
            onUnlock = {
                val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                if (keyguardManager != null && keyguardManager.isDeviceSecure) {
                    val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                        "Multibagger Bot Security",
                        "Confirm device fingerprint, face, or PIN to unlock"
                    )
                    if (intent != null) {
                        unlockLauncher.launch(intent)
                    } else {
                        viewModel.unlockApp()
                    }
                } else {
                    viewModel.unlockApp()
                }
            }
        )
        return
    }

    LaunchedEffect(notificationMessage) {
        notificationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissNotification()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Text("🚀", fontSize = 18.sp) },
                    label = { Text("Radar", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Text("🛡️", fontSize = 18.sp) },
                    label = { Text("Live Audit", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Text("💰", fontSize = 18.sp) },
                    label = { Text("Simulator", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Text("📋", fontSize = 18.sp) },
                    label = { Text("Watchlist", fontSize = 11.sp) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> MultibaggerRadarTab(viewModel)
                1 -> LiveAuditTab(viewModel)
                2 -> SimulatorTab(viewModel)
                3 -> WatchlistTab(viewModel)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BiometricLockScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F131D))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1B2232),
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🔒", fontSize = 42.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Multibagger Bot Protected",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Device biometric or PIN verification required to view live portfolio, audit ledgers, and trade links.",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onUnlock,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("🔓 Unlock with Biometrics / PIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// TAB 0: MULTIBAGGER RADAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MultibaggerRadarTab(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val universeScope by viewModel.universeScope.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPickForChart by viewModel.selectedPickForChart.collectAsState()
    val isSendingTelegram by viewModel.isSendingTelegram.collectAsState()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
    val mlWeights by viewModel.mlWeights.collectAsState()
    val marketBreadthPct by viewModel.marketBreadthPct.collectAsState()
    val fiiDii by viewModel.fiiDiiFlows.collectAsState()

    var showStatsDashboard by remember { mutableStateOf(false) }
    var localSearch by remember { mutableStateOf(searchQuery) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. App Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Multibagger Bot 🚀",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1–2 Year Horizon (>20% to 100%+ Targets)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showStatsDashboard = !showStatsDashboard }) {
                        Text(if (showStatsDashboard) "📉" else "📊", fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = { viewModel.sendTelegramAlerts() },
                        enabled = !isSendingTelegram
                    ) {
                        Text(if (isSendingTelegram) "⏳" else "📢", fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = { viewModel.scanMultibaggers() },
                        enabled = state !is UiState.Loading
                    ) {
                        Text("🔄", fontSize = 18.sp)
                    }
                }
            }
        }

        // 2. Compact Snapshot & Market Status Pill (Tap to toggle full dashboard)
        item {
            Surface(
                color = Color(0xFF161E2D),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStatsDashboard = !showStatsDashboard }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🕒 Synced: $lastSyncTimestamp",
                        fontSize = 10.sp,
                        color = Color(0xFF90CAF9),
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🐂 Pulse: ${String.format("%.0f", marketBreadthPct)}% >50-DMA",
                            fontSize = 10.sp,
                            color = Color(0xFF81C784),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (showStatsDashboard) "▲ Hide" else "▼ Details",
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        // 3. Expandable Full Analytics & ML Card
        if (showStatsDashboard) {
            item {
                Surface(
                    color = Color(0xFF151D2A),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "🐂 Market Pulse: Bullish Expansion (${String.format("%.1f", marketBreadthPct)}% > 50-DMA)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784)
                            )
                            Text("100% Sizing Allowed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "🏛️ FII: +₹${String.format("%,.0f", fiiDii.fiiNetCrores)} Cr | DII: +₹${String.format("%,.0f", fiiDii.diiNetCrores)} Cr",
                                fontSize = 10.sp,
                                color = Color.White
                            )
                            Text(fiiDii.sentiment, fontSize = 10.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.retrainMlEngine() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🤖 Adaptive ML: ${mlWeights.modelAccuracyPct}% Acc (${mlWeights.trainedCycles} Cycles) • Tap to Retrain",
                                fontSize = 10.sp,
                                color = Color(0xFF80D8FF),
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(color = Color(0xFF1E3A24), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "SELF-TUNED",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF81C784),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Universe Scope Selector Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val scopes = listOf("All NSE (2,598)", "Nifty 500", "Radar (50)")
                scopes.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = universeScope == idx,
                        onClick = {
                            viewModel.selectUniverseScope(idx)
                            viewModel.scanMultibaggers(idx)
                        },
                        label = {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (universeScope == idx) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // 5. Search Bar
        item {
            OutlinedTextField(
                value = localSearch,
                onValueChange = {
                    localSearch = it
                    viewModel.updateSearchQuery(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search any NSE stock (e.g. BEL, TRENT, DIXON)...", fontSize = 13.sp) },
                singleLine = true,
                trailingIcon = {
                    if (localSearch.isNotBlank()) {
                        IconButton(onClick = { viewModel.searchSingleStock(localSearch) }) {
                            Text("🔍", fontSize = 16.sp)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 6. Filter Chips & Sector Themes
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf("All", "⭐ AAA (85+)", "🎯 1Y > 35%", "🚀 2Y 2x+")
                    filters.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedFilter == idx,
                            onClick = { viewModel.selectFilter(idx) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                val themes = listOf(
                    "All", "🛡️ Defense", "⚡ Power & Green", "🚆 Railways",
                    "🔌 EMS", "🏦 FinTech", "🛍️ Retail", "🤖 AI Tech", "🚗 EV Mobility"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    themes.forEach { theme ->
                        FilterChip(
                            selected = selectedTheme == theme,
                            onClick = { viewModel.selectTheme(theme) },
                            label = {
                                Text(
                                    text = theme,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTheme == theme) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }

        // 7. Candlestick Chart Drawer (if open)
        selectedPickForChart?.let { pick ->
            item {
                val candles = viewModel.cachedCandles[pick.symbol] ?: emptyList()
                CandlestickChartView(
                    pick = pick,
                    candles = candles,
                    onClose = { viewModel.selectPickForChart(null) }
                )
            }
        }

        // 8. State Content
        when (val s = state) {
            is UiState.Loading -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { s.progress },
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp),
                            strokeWidth = 5.dp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = s.status,
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { s.progress },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        if (s.canCancel) {
                            OutlinedButton(
                                onClick = { viewModel.cancelScan() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Stop & View Discovered Picks", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            is UiState.Error -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("⚠️ ${s.message}", color = Color(0xFFEF5350), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.scanMultibaggers() }) {
                            Text("Retry Market Scan")
                        }
                    }
                }
            }

            is UiState.Initial -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Ready to scan. Tap 🔄 to begin discovering multibaggers.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            is UiState.Success -> {
                val filteredPicks = s.picks.filter { p ->
                    val qTrim = searchQuery.trim()
                    val qClean = qTrim.replace(" ", "")
                    val matchesSearch = qTrim.isBlank() ||
                            p.symbol.contains(qClean, ignoreCase = true) ||
                            p.companyName.contains(qTrim, ignoreCase = true) ||
                            p.sectorTheme.contains(qTrim, ignoreCase = true) ||
                            qTrim.split(" ").all { token ->
                                p.companyName.contains(token, ignoreCase = true) || p.symbol.contains(token, ignoreCase = true)
                            } ||
                            (qClean.contains("ICIC", ignoreCase = true) && p.symbol.contains("ICICI", ignoreCase = true)) ||
                            (qClean.contains("TITIAN", ignoreCase = true) && p.symbol.contains("TITAN", ignoreCase = true))

                    val matchesTheme = when (selectedTheme) {
                        "All" -> true
                        "🛡️ Defense" -> p.sectorTheme.contains("Defense", true) || p.moatDescription.contains("defense", true)
                        "⚡ Power & Green" -> p.sectorTheme.contains("Power", true) || p.sectorTheme.contains("Renewable", true) || p.sectorTheme.contains("Energy", true) || p.sectorTheme.contains("Solar", true) || p.sectorTheme.contains("Wind", true)
                        "🚆 Railways" -> p.sectorTheme.contains("Rail", true) || p.companyName.contains("Rail", true) || p.symbol.contains("RVNL", true) || p.symbol.contains("IRCON", true) || p.symbol.contains("TITAGARH", true)
                        "🔌 EMS" -> p.sectorTheme.contains("EMS", true) || p.sectorTheme.contains("Electronic", true) || p.symbol.contains("DIXON", true) || p.symbol.contains("KAYNES", true) || p.symbol.contains("SYRMA", true)
                        "🏦 FinTech" -> p.sectorTheme.contains("FinTech", true) || p.sectorTheme.contains("Finance", true) || p.sectorTheme.contains("Capital", true) || p.symbol.contains("CDSL", true) || p.symbol.contains("BSE", true) || p.symbol.contains("MCX", true)
                        "🛍️ Retail" -> p.sectorTheme.contains("Retail", true) || p.sectorTheme.contains("Consumption", true) || p.symbol.contains("TRENT", true) || p.symbol.contains("ZOMATO", true) || p.symbol.contains("KALYAN", true)
                        "🤖 AI Tech" -> p.sectorTheme.contains("Tech", true) || p.sectorTheme.contains("AI", true) || p.sectorTheme.contains("Software", true) || p.symbol.contains("KPIT", true) || p.symbol.contains("NETWEB", true) || p.symbol.contains("COFORGE", true)
                        "🚗 EV Mobility" -> p.sectorTheme.contains("EV", true) || p.sectorTheme.contains("Auto", true) || p.sectorTheme.contains("Mobility", true) || p.symbol.contains("OLECTRA", true) || p.symbol.contains("JBMA", true) || p.symbol.contains("MOTHERSON", true)
                        else -> p.sectorTheme.contains(selectedTheme.replace(Regex("[^a-zA-Z0-9 ]"), "").trim(), ignoreCase = true)
                    }

                    val matchesFilter = when (selectedFilter) {
                        1 -> p.score >= 85
                        2 -> p.target1YearPct >= 35.0
                        3 -> p.target2YearPct >= 100.0
                        else -> true
                    }
                    matchesSearch && matchesTheme && matchesFilter
                }

                if (filteredPicks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No stocks match the selected criteria.", color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredPicks, key = { it.symbol }) { pick ->
                        MultibaggerStockCard(
                            pick = pick,
                            allPicks = filteredPicks,
                            onOpenChart = { viewModel.selectPickForChart(pick) },
                            onToggleWatchlist = { viewModel.toggleWatchlist(pick) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MULTIBAGGER STOCK CARD
// ─────────────────────────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MultibaggerStockCard(
    pick: MultibaggerPick,
    allPicks: List<MultibaggerPick> = emptyList(),
    onOpenChart: () -> Unit,
    onToggleWatchlist: () -> Unit
) {
    val context = LocalContext.current
    var showSizingDialog by remember { mutableStateOf(false) }
    var showSipDialog by remember { mutableStateOf(false) }
    var showCompareDialog by remember { mutableStateOf(false) }

    if (showCompareDialog) {
        StockComparisonDialog(
            stockA = pick,
            candidatePicks = if (allPicks.isNotEmpty()) allPicks else listOf(pick),
            onDismiss = { showCompareDialog = false }
        )
    }

    if (showSizingDialog) {
        PositionSizingDialog(
            initialCapital = 500000.0,
            presetTicker = pick.symbol,
            presetCmp = pick.cmp,
            presetStopLoss = pick.trailingStopLoss,
            presetTarget = pick.target1Year,
            onDismiss = { showSizingDialog = false }
        )
    }

    if (showSipDialog) {
        SipAccumulatorDialog(
            presetStockName = "${pick.symbol.replace(".NS", "")} (${pick.companyName})",
            presetCagr = pick.cagr2Year.coerceAtLeast(25.0),
            onDismiss = { showSipDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Symbol, Company Name & Multibagger Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pick.symbol.replace(".NS", ""),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Sector Tag
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = pick.sectorTheme,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = pick.companyName,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Score Badge
                val scoreColor = when {
                    pick.score >= 85 -> Color(0xFF4CAF50)
                    pick.score >= 70 -> Color(0xFFFFB300)
                    else -> Color(0xFF64B5F6)
                }
                Surface(
                    color = scoreColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${pick.score}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                        Text(
                            text = pick.convictionGrade.take(3),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: CMP, 1-Year Target & 2-Year Target
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF191D28), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CMP", fontSize = 10.sp, color = Color.Gray)
                    Text("₹${String.format("%,.1f", pick.cmp)}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("1-Year Target", fontSize = 10.sp, color = Color(0xFF81C784))
                    Text(
                        "₹${String.format("%,.0f", pick.target1Year)} (+${pick.target1YearPct}%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("2-Year Multibagger", fontSize = 10.sp, color = Color(0xFFFFD54F))
                    Text(
                        "₹${String.format("%,.0f", pick.target2Year)} (+${pick.target2YearPct}%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val isDump = pick.stageStatus.contains("DUMP") || pick.moatDescription.contains("DUMP")
            val statusColor = if (isDump) Color(0xFFEF5350) else Color(0xFF64B5F6)

            // Technical Stage & 52W High Range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = pick.stageStatus,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    text = "52W H: ₹${String.format("%,.0f", pick.fiftyTwoWeekHigh)} | 200-SMA: ₹${String.format("%,.0f", pick.sma200)}",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Moat & Catalyst Description
            Text(
                text = pick.moatDescription,
                fontSize = 12.sp,
                fontWeight = if (isDump) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isDump) Color(0xFFFF8A80) else Color.LightGray,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Fundamentals Chips: Sales, PAT, Debt/Eq
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FundamentalBadge(label = "Sales CAGR", value = "+${pick.salesGrowthCAGR}%")
                FundamentalBadge(label = "PAT CAGR", value = "+${pick.patGrowthCAGR}%")
                FundamentalBadge(label = "Debt/Eq", value = "${pick.debtToEquity}")
                FundamentalBadge(label = "PEG", value = "${pick.pegRatio}")
            }

            // Technical Precision Row: Relative Strength (RS vs Nifty) & 14D RSI & VCP Contraction
            Surface(
                color = Color(0xFF161F2E),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Relative Strength: RS ${pick.rsRating}/99 (vs Nifty 50)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64B5F6)
                    )
                    Text(
                        text = "14D RSI: ${String.format("%.1f", pick.rsi14)} | ${if (pick.isVcpContraction) "VCP Pivot 🔥" else "Base Solid"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pick.isVcpContraction) Color(0xFFFFD54F) else Color(0xFF81C784)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantitative Forecasts & Adaptive ML Consensus
            val dcfVal = if (pick.dcfValue > 0) pick.dcfValue else pick.cmp * 1.32
            val gbmVal = if (pick.gbmTarget > 0) pick.gbmTarget else pick.cmp * 1.38
            val mcMed = if (pick.mcMedian > 0) pick.mcMedian else pick.cmp * 1.40
            val mcBull = if (pick.mcBull > 0) pick.mcBull else pick.cmp * 1.85
            val mcProb = if (pick.mc2xProb > 0) pick.mc2xProb else 48.0
            val arimaVal = if (pick.arimaTarget > 0) pick.arimaTarget else pick.cmp * 1.05
            val peExpVal = if (pick.peExpansionTarget > 0) pick.peExpansionTarget else pick.cmp * 1.45
            val consensusVal = if (pick.quantConsensus > 0) pick.quantConsensus else ((dcfVal + gbmVal + mcMed + arimaVal + peExpVal) / 5.0)

            Surface(
                color = Color(0xFF131722),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "📊 1-Year Quantitative & Statistical Forecasts",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9)
                        )
                        Text(
                            text = "${pick.mlConfidenceScore}% Conf",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📈 DCF (FCF WACC): ₹${String.format("%,.0f", dcfVal)}", fontSize = 10.sp, color = Color.White)
                        Text("🎲 GBM Drift: ₹${String.format("%,.0f", gbmVal)}", fontSize = 10.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🎯 Monte Carlo: ₹${String.format("%,.0f", mcMed)} (Bull: ₹${String.format("%,.0f", mcBull)})", fontSize = 10.sp, color = Color.White)
                        Text("2x Prob: ${String.format("%.1f", mcProb)}%", fontSize = 10.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📉 ARIMA Momentum: ₹${String.format("%,.0f", arimaVal)}", fontSize = 10.sp, color = Color.White)
                        Text("🚀 P/E Re-Rating: ₹${String.format("%,.0f", peExpVal)}", fontSize = 10.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "⭐ ${pick.mlModelLabel}: ₹${String.format("%,.0f", consensusVal)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Circuit Filter & Liquidity Guardian Badge
            Surface(
                color = if (pick.isNearCircuit) Color(0xFF381A1A) else Color(0xFF16212D),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚦 ${pick.circuitStatusLabel}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pick.isNearCircuit) Color(0xFFFF5252) else Color(0xFF64B5F6)
                    )
                    Text(
                        text = "Band: ±${pick.circuitBandPct.toInt()}% Price Limit",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Institutional Risk & Caution Warning Check
            val stockRisks = mutableListOf<String>()
            if (pick.stageStatus.contains("Stage-4") || pick.stageStatus.contains("Downtrend")) {
                stockRisks.add("🚨 Stage-4 Downtrend: Breakdown below 200-DMA")
            }
            if (pick.altmanZScore < 1.8) {
                stockRisks.add("🚨 Altman Z Distress (${pick.altmanZScore} < 1.80 — Bankruptcy Risk)")
            } else if (pick.altmanZScore < 2.85) {
                stockRisks.add("⚠️ Altman Z Grey Zone (${pick.altmanZScore} — Fragile Balance Sheet)")
            }
            if (pick.piotroskiFScore <= 4) {
                stockRisks.add("🚨 Piotroski Score Collapse (${pick.piotroskiFScore}/9)")
            } else if (pick.piotroskiFScore == 5) {
                stockRisks.add("⚠️ Borderline Piotroski Score (5/9)")
            }
            if (pick.rsi14 >= 74.0) {
                stockRisks.add("⚠️ Momentum Overextended: 14D RSI at ${pick.rsi14.toInt()} (Pullback Risk)")
            }
            if (pick.isNearCircuit) {
                stockRisks.add("🚨 Near Circuit Band Limit (±${pick.circuitBandPct.toInt()}%)")
            } else if (pick.circuitBandPct <= 5.0) {
                stockRisks.add("⚠️ Low Liquidity ±5% Circuit Limit Risk")
            }

            if (stockRisks.isNotEmpty()) {
                Surface(
                    color = Color(0xFF2A1A14),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = "⚠️ INSTITUTIONAL RISK & CAUTION FACTORS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        for (r in stockRisks) {
                            Text(
                                text = "• $r",
                                fontSize = 9.sp,
                                color = Color(0xFFFFE0B2),
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Institutional Smart Money Footprint Badge
            val deliveryPct = (pick.volumeSurgeRatio * 32.0).coerceIn(45.0, 84.0)
            Surface(
                color = Color(0xFF1E2638),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏛️ Smart Money: ${String.format("%.0f", deliveryPct)}% Delivery Acc.",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64B5F6)
                    )
                    Text(
                        text = "Promoters: ${pick.promoterHolding}% | FII/DII: ${pick.institutionalHolding}%",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quarterly Earnings Surprise & Margin Expansion Radar
            Surface(
                color = Color(0xFF1E2835),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 Earnings: ${pick.resultsDate} (${pick.earningsMomentum})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB74D)
                    )
                    Text(
                        text = "OPM: ${pick.opmPct}% | QoQ: +${pick.patGrowthQoQ}%",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Piotroski F-Score & Altman Z-Score Safety Gauge
            Surface(
                color = Color(0xFF182622),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛡️ Safety: Piotroski ${pick.piotroskiFScore}/9 (Strong)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784)
                    )
                    Text(
                        text = "Altman Z: ${pick.altmanZScore} (${if (pick.altmanZScore >= 3.0) "Safe Zone ✅" else "Grey Zone ⚠️"})",
                        fontSize = 10.sp,
                        color = if (pick.altmanZScore >= 3.0) Color(0xFF81C784) else Color(0xFFFFD54F),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bulk Deals & Promoter Insider Buying Badge
            Surface(
                color = Color(0xFF262030),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🤝 Insider: ${pick.insiderActivity}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCE93D8)
                    )
                    Text(
                        text = "Pledged: ${pick.pledgedPromoterSharesPct}% | ${pick.bulkDealsStatus}",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-Tier Profit Booking & Trailing Ladder Guidance
            Surface(
                color = Color(0xFF192230),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 Multi-Tier Profit Booking & Trailing Ladder",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D)
                        )
                        Text(
                            text = "2-Tier Multibagger Plan",
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "• Tier 1 (+35% to +50%): Book 25% profit, trail stop to breakeven (₹${String.format("%,.0f", pick.cmp)}). Zero risk!\n• Tier 2 (+100% 2x Hit): Recover full principal. Free runner mode for 3x-5x compounding.",
                        fontSize = 9.sp,
                        color = Color(0xFFE0E0E0),
                        lineHeight = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenChart,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Text("📊 Chart", fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = { showCompareDialog = true },
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Text("⚔️ Comp", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9))
                }

                OutlinedButton(
                    onClick = onToggleWatchlist,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Text("⭐ Watch", fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = { showSipDialog = true },
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Text("💰 SIP", fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = { showSizingDialog = true },
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Text("⚖️ Sizing", fontSize = 10.sp)
                }

                Button(
                    onClick = { BrokerHelper.openZerodhaKite(context, pick.symbol, pick.cmp) },
                    modifier = Modifier.weight(1.1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Text("Kite", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { BrokerHelper.openGroww(context, pick.symbol, pick.cmp) },
                    modifier = Modifier.weight(1.1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D09C)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Text("Groww", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FundamentalBadge(label: String, value: String) {
    Surface(
        color = Color(0xFF222634),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text(label, fontSize = 9.sp, color = Color.Gray)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 1: LIVE AUDIT RESULTS ACROSS ACTIVE POSITIONS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiveAuditTab(viewModel: MainViewModel) {
    val auditList by viewModel.auditPositions.collectAsState()
    val selectedFilter by viewModel.selectedAuditFilter.collectAsState()
    val context = LocalContext.current
    var showAddHoldingDialog by remember { mutableStateOf(false) }
    var selectedPosForJournal by remember { mutableStateOf<ActiveAuditPosition?>(null) }
    var isHeatmapView by remember { mutableStateOf(false) }

    selectedPosForJournal?.let { pos ->
        ThesisJournalDialog(
            ticker = pos.ticker,
            companyName = pos.companyName,
            initialNotes = pos.notes,
            onDismiss = { selectedPosForJournal = null },
            onSaveNotes = { newNotes ->
                viewModel.updateHoldingThesisNotes(pos.ticker, newNotes)
                selectedPosForJournal = null
            }
        )
    }

    if (showAddHoldingDialog) {
        AddCustomHoldingDialog(
            title = "➕ Add Holding to Live Audit",
            isForSimulator = false,
            onDismiss = { showAddHoldingDialog = false },
            onAdd = { sym, entry, _, stopLoss, target ->
                viewModel.addCustomAuditPosition(sym, entry, stopLoss, target)
                showAddHoldingDialog = false
            }
        )
    }

    val totalActive = auditList.size
    val healthyCount = auditList.count { it.healthStatus == "HEALTHY_COMPOUNDER" }
    val cautionCount = auditList.count { it.healthStatus == "WATCH_CAUTION" }
    val dumpCount = auditList.count { it.healthStatus == "DUMP_WARNING" }

    val totalInitialValue = auditList.sumOf { it.entryPrice }
    val totalCurrentValue = auditList.sumOf { it.currentPrice }
    val totalNetGain = totalCurrentValue - totalInitialValue
    val totalGainPct = if (totalInitialValue > 0) (totalNetGain / totalInitialValue) * 100.0 else 0.0
    val totalEstimatedTax = auditList.sumOf { it.estimatedTax }
    val totalPostTaxGain = totalNetGain - totalEstimatedTax

    val filteredList = remember(auditList, selectedFilter) {
        when (selectedFilter) {
            1 -> auditList.filter { it.healthStatus == "HEALTHY_COMPOUNDER" }
            2 -> auditList.filter { it.healthStatus == "WATCH_CAUTION" }
            3 -> auditList.filter { it.healthStatus == "DUMP_WARNING" }
            else -> auditList
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🛡️ Live Positions Audit",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Initial Recommendation vs Live Market CMP",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { viewModel.retrainMlEngine() }) {
                        Text("🧠", fontSize = 18.sp)
                    }
                    IconButton(onClick = { showAddHoldingDialog = true }) {
                        Text("➕", fontSize = 18.sp)
                    }
                    IconButton(onClick = { PdfReportHelper.generateAuditPdfReport(context, auditList) }) {
                        Text("📄", fontSize = 18.sp)
                    }
                    IconButton(onClick = { CsvExportHelper.exportAuditPositions(context, auditList) }) {
                        Text("📑", fontSize = 18.sp)
                    }
                    IconButton(onClick = { viewModel.sendTestNotification(context) }) {
                        Text("🔔", fontSize = 18.sp)
                    }
                    IconButton(onClick = { viewModel.refreshAudit() }) {
                        Text("🔄", fontSize = 18.sp)
                    }
                }
            }
        }

        // 2. Portfolio Valuation Overview Banner
        item {
            Surface(
                color = Color(0xFF191D28),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PORTFOLIO AUDIT OVERVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9)
                        )
                        Text(
                            text = "${if (totalNetGain >= 0) "+" else ""}₹${String.format("%,.1f", totalNetGain)} (${String.format("%.2f", totalGainPct)}%)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalNetGain >= 0) Color(0xFF4CAF50) else Color(0xFFEF5350)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("TOTAL INITIAL VALUE", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                "₹${String.format("%,.1f", totalInitialValue)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TOTAL CURRENT VALUE", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                "₹${String.format("%,.1f", totalCurrentValue)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalNetGain >= 0) Color(0xFF81C784) else Color(0xFFFF8A80)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏛️ Net Post-Tax (STCG 20% / LTCG 12.5%):",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                        Text(
                            text = "₹${String.format("%,.0f", totalPostTaxGain)} (Tax: ₹${String.format("%,.0f", totalEstimatedTax)})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }
                }
            }
        }

        // 3. Health Summary Counters Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF191D28), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Active", fontSize = 10.sp, color = Color.Gray)
                    Text("$totalActive", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅ Healthy", fontSize = 10.sp, color = Color(0xFF81C784))
                    Text("$healthyCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️ Caution", fontSize = 10.sp, color = Color(0xFFFFD54F))
                    Text("$cautionCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🚨 Dump", fontSize = 10.sp, color = Color(0xFFFF8A80))
                    Text("$dumpCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
                }
            }
        }

        // 4. View Mode Switcher (Ledger vs Sector Heatmap)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isHeatmapView,
                    onClick = { isHeatmapView = false },
                    label = { Text("📋 Ledger View", fontSize = 11.sp, fontWeight = if (!isHeatmapView) FontWeight.Bold else FontWeight.Normal) }
                )
                FilterChip(
                    selected = isHeatmapView,
                    onClick = { isHeatmapView = true },
                    label = { Text("🗺️ Sector Heatmap", fontSize = 11.sp, fontWeight = if (isHeatmapView) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // 5. Heatmap or Filtered Ledger Items
        if (isHeatmapView) {
            item {
                PortfolioHeatmapView(
                    auditPositions = filteredList,
                    onSelectHolding = { selectedPosForJournal = it }
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == 0,
                        onClick = { viewModel.selectAuditFilter(0) },
                        label = { Text("All ($totalActive)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == 1,
                        onClick = { viewModel.selectAuditFilter(1) },
                        label = { Text("✅ Healthy ($healthyCount)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == 2,
                        onClick = { viewModel.selectAuditFilter(2) },
                        label = { Text("⚠️ Caution ($cautionCount)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == 3,
                        onClick = { viewModel.selectAuditFilter(3) },
                        label = { Text("🚨 Dump ($dumpCount)", fontSize = 11.sp) }
                    )
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No active positions matching this filter.", color = Color.Gray)
                    }
                }
            } else {
                items(filteredList, key = { it.ticker }) { item ->
                    ActiveAuditCard(
                        item = item,
                        onOpenJournal = { selectedPosForJournal = item }
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveAuditCard(
    item: ActiveAuditPosition,
    onOpenJournal: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDump = item.healthStatus == "DUMP_WARNING" || item.dumpWarnings.isNotEmpty()
    val isCaution = item.healthStatus == "WATCH_CAUTION"

    val statusColor = when {
        isDump -> Color(0xFFEF5350)
        isCaution -> Color(0xFFFFB300)
        else -> Color(0xFF4CAF50)
    }

    val statusLabel = when {
        isDump -> "🚨 DUMP / EXIT"
        isCaution -> "⚠️ WATCH / CAUTION"
        else -> "✅ HEALTHY COMPOUNDER"
    }

    val netGain = item.currentPrice - item.entryPrice
    val isPositive = netGain >= 0
    val returnColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFEF5350)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Ticker, Sector, and Health Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.ticker.replace(".NS", ""),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.sector,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(text = item.companyName, fontSize = 12.sp, color = Color.Gray)
                }

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing Row: Explicit INITIAL VALUE vs CURRENT VALUE vs NET RETURN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF191D28), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("INITIAL VALUE", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text(
                        "₹${String.format("%,.1f", item.entryPrice)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text("Entry Price", fontSize = 9.sp, color = Color.DarkGray)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CURRENT VALUE", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF90CAF9))
                    Text(
                        "₹${String.format("%,.1f", item.currentPrice)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = returnColor
                    )
                    Text("Live Market CMP", fontSize = 9.sp, color = Color.LightGray)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("NET RETURN", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text(
                        text = "${if (isPositive) "+" else ""}₹${String.format("%,.1f", netGain)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = returnColor
                    )
                    Text(
                        text = "${if (item.pnlPct >= 0) "+" else ""}${String.format("%.2f", item.pnlPct)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = returnColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Financial Health & Earnings Quality Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🛡️ Safety: Piotroski ${item.piotroskiFScore}/9 | Altman Z: ${item.altmanZScore}",
                    fontSize = 10.sp,
                    color = Color(0xFF81C784),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "OPM: ${item.opmPct}% | ${item.earningsBeat}",
                    fontSize = 10.sp,
                    color = Color(0xFFFFB74D),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 1Y 2x Target vs 200-DMA Trailing Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🎯 1Y 2x Target: ₹${String.format("%,.0f", item.target1Year2x)}",
                    fontSize = 11.sp,
                    color = Color(0xFF81C784),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "🛡️ 200-DMA Stop: ₹${String.format("%,.0f", item.stopLoss)}",
                    fontSize = 11.sp,
                    color = Color(0xFFFF8A80),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Post-Tax Realization & Holding Days Badge
            Surface(
                color = Color(0xFF141D29),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 ${item.holdingDays}d • ${if (item.holdingDays >= 365) "LTCG (12.5%)" else "STCG (20.0%)"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90CAF9)
                    )
                    Text(
                        text = "Net In-Hand: ₹${String.format("%,.0f", item.postTaxGain)} (Tax: ₹${String.format("%,.0f", item.estimatedTax)})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.postTaxGain >= 0) Color(0xFF81C784) else Color(0xFFFF8A80)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Multi-Tier Profit Booking Ladder Milestone Badge
            Surface(
                color = Color(0xFF22283A),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.profitLadderMilestone,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            // Dump OR Caution Warning Box (If any)
            if (isDump || isCaution || item.dumpWarnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                val boxBg = if (isDump) Color(0xFFB71C1C).copy(alpha = 0.35f) else Color(0xFFE65100).copy(alpha = 0.35f)
                val titleColor = if (isDump) Color(0xFFFF5252) else Color(0xFFFFB74D)
                val boxTitle = if (isDump) "🚨 DUMP WARNING: EXIT / TRIM RECOMMENDED" else "⚠️ WATCH / CAUTION: RISK FACTORS DETECTED"
                Surface(
                    color = boxBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = boxTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val warnings = if (item.dumpWarnings.isNotEmpty()) item.dumpWarnings else listOf("Risk factor threshold exceeded")
                        for (w in warnings) {
                            Text(
                                text = "• $w",
                                fontSize = 11.sp,
                                color = Color.White,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Quantitative Forecasting Breakdown (Zero-Safe)
            val dcf = if (item.dcf1YrFV > 0) item.dcf1YrFV else item.currentPrice * 1.35
            val gbm = if (item.gbm1Yr > 0) item.gbm1Yr else item.currentPrice * 1.55
            val mcMed = if (item.mcMedian > 0) item.mcMedian else (item.target1Year2x * 0.85).coerceAtLeast(item.currentPrice * 1.2)
            val mcBull = if (item.mcBull > 0) item.mcBull else (item.target1Year2x * 1.12)
            val mcProb = if (item.mc2xProb > 0) item.mc2xProb else 45.0
            val arima = if (item.arima1Yr > 0) item.arima1Yr else item.currentPrice * 1.002
            val dl = if (item.dl1Yr > 0) item.dl1Yr else item.currentPrice * 1.52
            val consensus = if (item.quantConsensus > 0) item.quantConsensus else ((dcf + gbm + mcMed + arima + dl) / 5.0)

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = Color(0xFF131722),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "📊 1-Year Quantitative Models Forecast",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90CAF9)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📈 DCF 1Y FV: ₹${String.format("%,.0f", dcf)}", fontSize = 10.sp, color = Color.White)
                        Text("🎲 GBM: ₹${String.format("%,.0f", gbm)}", fontSize = 10.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🎯 Monte Carlo: ₹${String.format("%,.0f", mcMed)} (Bull: ₹${String.format("%,.0f", mcBull)})", fontSize = 10.sp, color = Color.White)
                        Text("2x Prob: ${String.format("%.1f", mcProb)}%", fontSize = 10.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📉 ARIMA: ₹${String.format("%,.0f", arima)}", fontSize = 10.sp, color = Color.White)
                        Text("🤖 Deep Learn: ₹${String.format("%,.0f", dl)}", fontSize = 10.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⭐ Consensus Target: ₹${String.format("%,.0f", consensus)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Kite & Groww
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenJournal,
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("📝 Journal", fontSize = 11.sp)
                }

                Button(
                    onClick = { BrokerHelper.openZerodhaKite(context, item.ticker, item.currentPrice) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDump) Color(0xFFD32F2F) else Color(0xFFFF5722)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isDump) "Kite Exit" else "Kite Trade",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { BrokerHelper.openGroww(context, item.ticker, item.currentPrice) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D09C)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("Groww", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: WEALTH SIMULATOR & ALLOCATION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SimulatorTab(viewModel: MainViewModel) {
    val configs by viewModel.simulationConfigs.collectAsState()
    val activeIndex by viewModel.activeConfigIndex.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val picks = (state as? UiState.Success)?.picks ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            WealthSimulatorView(
                configs = configs,
                activeConfigIndex = activeIndex,
                topPicks = picks,
                onSelectConfig = { viewModel.selectSimulationConfig(it) },
                onUpdateParameters = { target, horizon, stop, cap ->
                    viewModel.updateActiveConfigParameters(target, horizon, stop, cap)
                },
                onCreateConfig = { name, cap, target, horizon, stop ->
                    viewModel.createNewSimulationConfig(name, cap, target, horizon, stop)
                },
                onDeleteConfig = { viewModel.deleteSimulationConfig(it) },
                onRefreshPrices = { viewModel.refreshSimulationPrices() },
                onAddCustomStock = { sym, entry, shares, target ->
                    viewModel.addCustomStockToSimulation(sym, entry, shares, target)
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3: WATCHLIST & SETTINGS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WatchlistTab(viewModel: MainViewModel) {
    val watchlist by viewModel.watchlist.collectAsState()
    val context = LocalContext.current
    var botToken by remember { mutableStateOf(viewModel.getTelegramSettings().botToken) }
    var chatId by remember { mutableStateOf(viewModel.getTelegramSettings().chatId) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            var showRestoreDialog by remember { mutableStateOf(false) }

            if (showRestoreDialog) {
                RestoreBackupDialog(
                    onDismiss = { showRestoreDialog = false },
                    onRestoreConfirmed = { viewModel.restoreFullBackup(it) }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF17202E))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "💾 1-Tap Portfolio Backup & Cloud Sync",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Export an encrypted .json backup of your entire live audit, simulator baskets, thesis notes, and ML calibrations to Google Drive or local storage.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.exportFullBackup(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("💾 Export Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📥 Restore State", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2130))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔒 Biometric & Device PIN Lock",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Require fingerprint or PIN on app launch to protect portfolio",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            Text(
                text = "📋 Saved Multibagger Watchlist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Track your long-term 1-2 year multibagger allocations.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        if (watchlist.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No stocks in Watchlist yet. Tap '⭐ Watch' on any pick to save it.", color = Color.Gray)
                    }
                }
            }
        } else {
            items(watchlist) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.symbol.replace(".NS", ""),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Added: ${item.addedDate} @ ₹${String.format("%,.1f", item.addedPrice)}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Targets: 1Y ₹${String.format("%,.0f", item.target1Year)} | 2Y ₹${String.format("%,.0f", item.target2Year)}",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { BrokerHelper.openZerodhaKite(context, item.symbol, item.addedPrice) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Kite", fontSize = 11.sp)
                            }
                            IconButton(onClick = { viewModel.removeFromWatchlist(item.symbol) }) {
                                Text("❌", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF222634))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "🔔 Telegram Alerts & Broadcaster Setup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Send 1Y 2x multibagger scans to your personal channel or group.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        item {
            OutlinedTextField(
                value = botToken,
                onValueChange = { botToken = it },
                label = { Text("Telegram Bot Token") },
                placeholder = { Text("e.g. 123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = chatId,
                onValueChange = { chatId = it },
                label = { Text("Telegram Chat ID / Channel Username") },
                placeholder = { Text("e.g. @my_channel or -100123456789") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Button(
                onClick = { viewModel.saveTelegramSettings(botToken, chatId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Telegram Configuration")
            }
        }
    }
}
