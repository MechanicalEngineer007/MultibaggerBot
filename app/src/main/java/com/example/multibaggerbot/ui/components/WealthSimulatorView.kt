package com.example.multibaggerbot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.window.Dialog
import com.example.multibaggerbot.data.BrokerHelper
import com.example.multibaggerbot.data.CsvExportHelper
import com.example.multibaggerbot.data.MultibaggerPick
import com.example.multibaggerbot.data.SimulationConfig
import com.example.multibaggerbot.data.SimulationStockItem
import kotlin.math.roundToInt

@Composable
fun WealthSimulatorView(
    configs: List<SimulationConfig>,
    activeConfigIndex: Int,
    topPicks: List<MultibaggerPick>,
    onSelectConfig: (Int) -> Unit,
    onUpdateParameters: (targetPct: Double, horizonMonths: Int, stopLossPct: Double, capital: Double) -> Unit,
    onCreateConfig: (name: String, capital: Double, targetPct: Double, horizonMonths: Int, stopLossPct: Double) -> Unit,
    onDeleteConfig: (Int) -> Unit,
    onRefreshPrices: () -> Unit,
    onAddCustomStock: (symbol: String, entryPrice: Double, shares: Int, targetPrice: Double) -> Unit
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var showAddStockDialog by remember { mutableStateOf(false) }
    var showSizingDialog by remember { mutableStateOf(false) }
    var showSipDialog by remember { mutableStateOf(false) }

    val activeConfig = configs.getOrNull(activeConfigIndex) ?: configs.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "💰 Multi-Scenario Wealth Simulator",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Track Initial Value, Live Market Changes & Predicted Return",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // SIP Accumulator Button
                IconButton(onClick = { showSipDialog = true }) {
                    Text("💰", fontSize = 17.sp)
                }

                // Sizing Calculator Button
                IconButton(onClick = { showSizingDialog = true }) {
                    Text("⚖️", fontSize = 17.sp)
                }

                // Export CSV Button
                IconButton(onClick = {
                    if (activeConfig != null) {
                        CsvExportHelper.exportSimulationConfig(context, activeConfig)
                    }
                }) {
                    Text("📑", fontSize = 17.sp)
                }

                // Refresh CMP
                IconButton(onClick = onRefreshPrices) {
                    Text("🔄", fontSize = 17.sp)
                }
            }
        }

        // Configuration Switcher Tabs / Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            configs.forEachIndexed { idx, cfg ->
                val isSelected = idx == activeConfigIndex
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectConfig(idx) },
                    label = {
                        Text(
                            text = cfg.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // New Config Chip Button
            Surface(
                color = Color(0xFF232A3B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showCreateDialog = true }
            ) {
                Text(
                    text = "➕ New Config",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF90CAF9),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (activeConfig == null) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No simulation configurations found. Create one to begin tracking.", color = Color.Gray)
            }
            return
        }

        // Calculations for Active Configuration
        val initialInvested = if (activeConfig.stocks.isNotEmpty()) {
            activeConfig.stocks.sumOf { it.shares * it.initialPrice }
        } else activeConfig.initialCapital

        val currentTrackedValue = if (activeConfig.stocks.isNotEmpty()) {
            activeConfig.stocks.sumOf { it.shares * it.currentPrice }
        } else activeConfig.initialCapital

        val regularChangeValue = currentTrackedValue - initialInvested
        val regularChangePct = if (initialInvested > 0) (regularChangeValue / initialInvested) * 100.0 else 0.0

        // Predicted Future Value based on adjustable Target Return %
        val predictedValue = initialInvested * (1.0 + (activeConfig.targetReturnPct / 100.0))
        val predictedGain = predictedValue - initialInvested

        // Macro Dashboard Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with Name & Edit Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeConfig.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Created: ${activeConfig.createdDate} • ${activeConfig.timeHorizonMonths}M Horizon",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { showAdjustDialog = true }) {
                            Text("⚙️", fontSize = 16.sp)
                        }
                        if (configs.size > 1) {
                            IconButton(onClick = { onDeleteConfig(activeConfigIndex) }) {
                                Text("🗑️", fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Three-Column Valuation Row: Initial vs Current vs Predicted
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E2230), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Initial Invested
                    Column {
                        Text("INITIAL VALUE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(
                            text = "₹${String.format("%,.0f", initialInvested)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("Base Capital", fontSize = 9.sp, color = Color.DarkGray)
                    }

                    // Current Value (Regular Changes Tracked Live)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CURRENT VALUE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9))
                        Text(
                            text = "₹${String.format("%,.0f", currentTrackedValue)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (regularChangeValue >= 0) Color(0xFF81C784) else Color(0xFFFF8A80)
                        )
                        val sign = if (regularChangeValue >= 0) "+" else ""
                        Text(
                            text = "$sign₹${String.format("%,.0f", regularChangeValue)} ($sign${String.format("%.1f", regularChangePct)}%)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (regularChangeValue >= 0) Color(0xFF4CAF50) else Color(0xFFEF5350)
                        )
                    }

                    // Predicted Target Value
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PREDICTED VALUE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                        Text(
                            text = "₹${String.format("%,.0f", predictedValue)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300)
                        )
                        Text(
                            text = "+${activeConfig.targetReturnPct.toInt()}% Target",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Adjustable Parameter Highlights Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🎯 Target Gain: +${activeConfig.targetReturnPct.toInt()}% (+₹${String.format("%,.0f", predictedGain)})",
                        fontSize = 11.sp,
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "🛡️ Trailing Stop Floor: -${activeConfig.trailingStopLossPct.toInt()}%",
                        fontSize = 11.sp,
                        color = Color(0xFFFF8A80),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Adjustable Parameters Quick-Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ Adjustable Parameters",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = { showAdjustDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Customize Sliders", fontSize = 11.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick Target Presets
                    val presets = listOf(40.0, 75.0, 100.0, 150.0)
                    presets.forEach { p ->
                        val isCurrent = activeConfig.targetReturnPct.roundToInt() == p.roundToInt()
                        FilterChip(
                            selected = isCurrent,
                            onClick = {
                                onUpdateParameters(
                                    p,
                                    activeConfig.timeHorizonMonths,
                                    activeConfig.trailingStopLossPct,
                                    activeConfig.initialCapital
                                )
                            },
                            label = { Text("+${p.toInt()}%", fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // Stock Basket Live Tracking Table Header with "Add Stock" Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💼 Live Tracked Stock Basket (${activeConfig.stocks.size} Positions)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = { showAddStockDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF283593)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("➕ Add Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (activeConfig.stocks.isEmpty()) {
            Surface(
                color = Color(0xFF1E2230),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No stocks attached to this configuration basket. Tap '➕ Add Stock' to add holdings.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activeConfig.stocks.forEach { item ->
                    SimulationStockCard(item = item, targetReturnPct = activeConfig.targetReturnPct)
                }
            }
        }
    }

    // Modal: Parameter Adjustment Dialog
    if (showAdjustDialog && activeConfig != null) {
        AdjustParametersDialog(
            config = activeConfig,
            onDismiss = { showAdjustDialog = false },
            onApply = { target, horizon, stop, cap ->
                onUpdateParameters(target, horizon, stop, cap)
                showAdjustDialog = false
            }
        )
    }

    // Modal: Create New Configuration Dialog
    if (showCreateDialog) {
        CreateConfigDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, cap, target, horizon, stop ->
                onCreateConfig(name, cap, target, horizon, stop)
                showCreateDialog = false
            }
        )
    }

    // Modal: Add Custom Holding to Active Simulation Basket
    if (showAddStockDialog) {
        val cfgName = activeConfig?.name ?: "Basket"
        AddCustomHoldingDialog(
            title = "➕ Add Stock to '$cfgName'",
            isForSimulator = true,
            onDismiss = { showAddStockDialog = false },
            onAdd = { sym, entryPrice, shares, _, target ->
                onAddCustomStock(sym, entryPrice, shares, target)
            }
        )
    }

    // Modal: Position Sizing & Risk Management Calculator
    if (showSizingDialog) {
        PositionSizingDialog(
            initialCapital = activeConfig?.initialCapital ?: 500000.0,
            onDismiss = { showSizingDialog = false }
        )
    }

    // Modal: Multibagger SIP Wealth Accumulator
    if (showSipDialog) {
        SipAccumulatorDialog(
            presetStockName = activeConfig?.name ?: "Multibagger Megatrend Basket",
            onDismiss = { showSipDialog = false }
        )
    }
}

@Composable
fun SimulationStockCard(item: SimulationStockItem, targetReturnPct: Double) {
    val context = LocalContext.current
    val totalInitial = item.shares * item.initialPrice
    val totalCurrent = item.shares * item.currentPrice
    val gainRupees = totalCurrent - totalInitial
    val gainPct = if (totalInitial > 0) (gainRupees / totalInitial) * 100.0 else 0.0
    val isProfit = gainRupees >= 0
    val returnColor = if (isProfit) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val predictedStockVal = totalInitial * (1.0 + targetReturnPct / 100.0)

    Surface(
        color = Color(0xFF1B1F2C),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Ticker & Share Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.symbol.replace(".NS", ""),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(text = item.companyName, fontSize = 11.sp, color = Color.Gray)
                }

                Surface(
                    color = Color(0xFF283046),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${item.shares} shares",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF90CAF9),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing Row: Initial vs Current CMP vs Net Gain
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141722), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("INITIAL", fontSize = 9.sp, color = Color.Gray)
                    Text("₹${String.format("%,.1f", item.initialPrice)}", fontSize = 12.sp, color = Color.White)
                    Text("₹${String.format("%,.0f", totalInitial)}", fontSize = 10.sp, color = Color.LightGray)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CURRENT CMP", fontSize = 9.sp, color = Color(0xFF90CAF9))
                    Text("₹${String.format("%,.1f", item.currentPrice)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = returnColor)
                    Text("₹${String.format("%,.0f", totalCurrent)}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = returnColor)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("CHANGE (P&L)", fontSize = 9.sp, color = Color.Gray)
                    val sign = if (isProfit) "+" else ""
                    Text("$sign₹${String.format("%,.0f", gainRupees)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = returnColor)
                    Text("$sign${String.format("%.1f", gainPct)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = returnColor)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Predicted: ₹${String.format("%,.0f", predictedStockVal)}",
                    fontSize = 11.sp,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { BrokerHelper.openZerodhaKite(context, item.symbol, item.currentPrice) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Kite", fontSize = 10.sp)
                    }

                    Button(
                        onClick = { BrokerHelper.openGroww(context, item.symbol, item.currentPrice) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D09C)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Groww", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdjustParametersDialog(
    config: SimulationConfig,
    onDismiss: () -> Unit,
    onApply: (targetPct: Double, horizonMonths: Int, stopLossPct: Double, capital: Double) -> Unit
) {
    var targetPct by remember { mutableStateOf(config.targetReturnPct) }
    var horizonMonths by remember { mutableStateOf(config.timeHorizonMonths) }
    var stopLossPct by remember { mutableStateOf(config.trailingStopLossPct) }
    var capitalInput by remember { mutableStateOf(config.initialCapital.roundToInt().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2230),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "⚙️ Adjust Parameters: ${config.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Capital Input
                OutlinedTextField(
                    value = capitalInput,
                    onValueChange = { capitalInput = it },
                    label = { Text("Base Capital (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Target Return % Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Target Return (Predicted)", fontSize = 12.sp, color = Color.LightGray)
                        Text("+${targetPct.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                    }
                    Slider(
                        value = targetPct.toFloat(),
                        onValueChange = { targetPct = it.toDouble() },
                        valueRange = 20f..300f,
                        steps = 27
                    )
                }

                // Time Horizon Selector
                Column {
                    Text("Time Horizon", fontSize = 12.sp, color = Color.LightGray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val horizons = listOf(6, 12, 24, 36)
                        horizons.forEach { h ->
                            FilterChip(
                                selected = horizonMonths == h,
                                onClick = { horizonMonths = h },
                                label = { Text("${h}M", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Trailing Stop Loss Cushion
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Stop-Loss Cushion", fontSize = 12.sp, color = Color.LightGray)
                        Text("-${stopLossPct.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80))
                    }
                    Slider(
                        value = stopLossPct.toFloat(),
                        onValueChange = { stopLossPct = it.toDouble() },
                        valueRange = 5f..35f,
                        steps = 5
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val cap = capitalInput.toDoubleOrNull() ?: config.initialCapital
                            onApply(targetPct, horizonMonths, stopLossPct, cap)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Apply & Save")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateConfigDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, capital: Double, targetPct: Double, horizonMonths: Int, stopLossPct: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var capitalInput by remember { mutableStateOf("200000") }
    var targetPct by remember { mutableStateOf(100.0) }
    var horizonMonths by remember { mutableStateOf(12) }
    var stopLossPct by remember { mutableStateOf(15.0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2230),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "➕ Create Tracking Configuration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Configuration Name") },
                    placeholder = { Text("e.g. Aggressive Smallcap 2X") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = capitalInput,
                    onValueChange = { capitalInput = it },
                    label = { Text("Initial Capital (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Target Return % Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Target Return (Predicted)", fontSize = 12.sp, color = Color.LightGray)
                        Text("+${targetPct.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                    }
                    Slider(
                        value = targetPct.toFloat(),
                        onValueChange = { targetPct = it.toDouble() },
                        valueRange = 25f..250f,
                        steps = 8
                    )
                }

                // Time Horizon Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val horizons = listOf(6, 12, 24, 36)
                    horizons.forEach { h ->
                        FilterChip(
                            selected = horizonMonths == h,
                            onClick = { horizonMonths = h },
                            label = { Text("${h}M", fontSize = 11.sp) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val cap = capitalInput.toDoubleOrNull() ?: 200000.0
                            onCreate(name, cap, targetPct, horizonMonths, stopLossPct)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}
