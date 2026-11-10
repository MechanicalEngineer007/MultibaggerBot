package com.example.multibaggerbot.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MultibaggerRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("multibagger_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveCachedPicks(picks: List<MultibaggerPick>) = withContext(Dispatchers.IO) {
        val serialized = json.encodeToString(picks)
        prefs.edit().putString("cached_multibagger_picks", serialized).apply()
    }

    suspend fun getCachedPicks(): List<MultibaggerPick> = withContext(Dispatchers.IO) {
        val str = prefs.getString("cached_multibagger_picks", null)
        if (str != null) {
            try {
                val list = json.decodeFromString<List<MultibaggerPick>>(str)
                if (list.isNotEmpty()) return@withContext list
            } catch (e: Exception) {
                // fall back to asset bundle
            }
        }
        // Fallback to bundled latest scan in assets
        try {
            val assetStream = context.assets.open("initial_multibaggers.json")
            val content = assetStream.bufferedReader().use { it.readText() }
            val bundled = json.decodeFromString<List<MultibaggerPick>>(content)
            if (bundled.isNotEmpty()) {
                prefs.edit().putString("cached_multibagger_picks", content).apply()
                return@withContext bundled
            }
        } catch (e: Exception) {
            // No asset found
        }
        emptyList()
    }

    fun getAuditPositionsSync(): List<ActiveAuditPosition> {
        val str = prefs.getString("cached_audit_positions", null)
        if (str != null) {
            try {
                val list = json.decodeFromString<List<ActiveAuditPosition>>(str)
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                // fallback
            }
        }
        try {
            val assetStream = context.assets.open("audit_ledger.json")
            val content = assetStream.bufferedReader().use { it.readText() }
            val bundled = json.decodeFromString<List<ActiveAuditPosition>>(content)
            if (bundled.isNotEmpty()) {
                prefs.edit().putString("cached_audit_positions", content).apply()
                return bundled
            }
        } catch (e: Exception) {
            // No asset
        }
        return emptyList()
    }

    suspend fun getAuditPositions(): List<ActiveAuditPosition> = withContext(Dispatchers.IO) {
        getAuditPositionsSync()
    }

    suspend fun saveAuditPositions(positions: List<ActiveAuditPosition>) = withContext(Dispatchers.IO) {
        val serialized = json.encodeToString(positions)
        prefs.edit().putString("cached_audit_positions", serialized).apply()
    }

    suspend fun getWatchlist(): List<WatchlistStock> = withContext(Dispatchers.IO) {
        val str = prefs.getString("saved_watchlist", null) ?: return@withContext emptyList()
        try {
            json.decodeFromString<List<WatchlistStock>>(str)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addToWatchlist(item: WatchlistStock) = withContext(Dispatchers.IO) {
        val current = getWatchlist().toMutableList()
        current.removeAll { it.symbol == item.symbol }
        current.add(0, item)
        val serialized = json.encodeToString(current)
        prefs.edit().putString("saved_watchlist", serialized).apply()
    }

    suspend fun removeFromWatchlist(symbol: String) = withContext(Dispatchers.IO) {
        val current = getWatchlist().toMutableList()
        current.removeAll { it.symbol == symbol }
        val serialized = json.encodeToString(current)
        prefs.edit().putString("saved_watchlist", serialized).apply()
    }

    suspend fun getAllNseSymbols(): List<NseSymbolItem> = withContext(Dispatchers.IO) {
        try {
            val assetStream = context.assets.open("all_nse_symbols.json")
            val content = assetStream.bufferedReader().use { it.readText() }
            json.decodeFromString<List<NseSymbolItem>>(content)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSimulationConfigs(): List<SimulationConfig> = withContext(Dispatchers.IO) {
        val str = prefs.getString("saved_simulation_configs", null)
        if (str != null) {
            try {
                val list = json.decodeFromString<List<SimulationConfig>>(str)
                if (list.isNotEmpty()) return@withContext list
            } catch (e: Exception) {
                // fallback
            }
        }
        val defaultConfigs = listOf(
            SimulationConfig(
                id = "config-1-aggressive",
                name = "🚀 2x High-Beta Aggressive",
                initialCapital = 200000.0,
                targetReturnPct = 100.0,
                timeHorizonMonths = 12,
                trailingStopLossPct = 15.0,
                createdDate = "08 Nov 2026",
                notes = "High-growth Stage-2 breakouts targeting 2x return in 1 year",
                stocks = listOf(
                    SimulationStockItem("MCX.NS", "Multi Commodity Exchange", 3249.0, 3528.0, 12, 6498.0, 8900.0),
                    SimulationStockItem("BSE.NS", "BSE Limited", 3490.0, 3800.0, 11, 6980.0, 9500.0),
                    SimulationStockItem("ANGELONE.NS", "Angel One", 290.0, 331.0, 137, 580.0, 800.0),
                    SimulationStockItem("TRENT.NS", "Trent Ltd (Tata Retail)", 2850.0, 3079.0, 14, 5700.0, 7800.0),
                    SimulationStockItem("DIXON.NS", "Dixon Tech", 12100.0, 12625.0, 3, 24200.0, 33000.0)
                )
            ),
            SimulationConfig(
                id = "config-2-compounder",
                name = "🛡️ Steady Wealth Compounder",
                initialCapital = 500000.0,
                targetReturnPct = 40.0,
                timeHorizonMonths = 12,
                trailingStopLossPct = 12.0,
                createdDate = "08 Nov 2026",
                notes = "Clean balance sheet leaders with low debt & high ROCE",
                stocks = listOf(
                    SimulationStockItem("BEL.NS", "Bharat Electronics", 295.0, 308.0, 338, 410.0, 575.0),
                    SimulationStockItem("HAL.NS", "Hindustan Aeronautics", 4855.0, 4980.0, 20, 6800.0, 9700.0),
                    SimulationStockItem("POLYCAB.NS", "Polycab India", 6650.0, 6820.0, 15, 9300.0, 13300.0),
                    SimulationStockItem("MANKIND.NS", "Mankind Pharma", 2481.0, 2540.0, 40, 3470.0, 4960.0),
                    SimulationStockItem("COFORGE.NS", "Coforge Limited", 2099.0, 2076.0, 47, 2930.0, 4190.0)
                )
            ),
            SimulationConfig(
                id = "config-3-smallcap",
                name = "⚡ Smallcap Rocket 2Y Basket",
                initialCapital = 150000.0,
                targetReturnPct = 125.0,
                timeHorizonMonths = 24,
                trailingStopLossPct = 18.0,
                createdDate = "08 Nov 2026",
                notes = "Emerging EMS, clean energy, and capital goods innovators",
                stocks = listOf(
                    SimulationStockItem("KAYNES.NS", "Kaynes Technology", 3100.0, 3233.0, 10, 6200.0, 8500.0),
                    SimulationStockItem("SUZLON.NS", "Suzlon Energy", 36.5, 38.8, 821, 73.0, 100.0),
                    SimulationStockItem("COCHINSHIP.NS", "Cochin Shipyard", 1180.0, 1220.0, 25, 2360.0, 3240.0),
                    SimulationStockItem("KALYANKJIL.NS", "Kalyan Jewellers", 505.4, 590.7, 59, 1010.0, 1400.0),
                    SimulationStockItem("TITAGARH.NS", "Titagarh Rail Systems", 695.7, 677.8, 43, 1390.0, 1910.0)
                )
            )
        )
        saveSimulationConfigs(defaultConfigs)
        defaultConfigs
    }

    suspend fun saveSimulationConfigs(configs: List<SimulationConfig>) = withContext(Dispatchers.IO) {
        val serialized = json.encodeToString(configs)
        prefs.edit().putString("saved_simulation_configs", serialized).apply()
    }

    fun getTelegramConfig(): TelegramConfig {
        val token = prefs.getString("tg_bot_token", "") ?: ""
        val chatId = prefs.getString("tg_chat_id", "") ?: ""
        val enabled = prefs.getBoolean("tg_enabled", false)
        return TelegramConfig(token, chatId, enabled)
    }

    fun saveTelegramConfig(config: TelegramConfig) {
        prefs.edit()
            .putString("tg_bot_token", config.botToken)
            .putString("tg_chat_id", config.chatId)
            .putBoolean("tg_enabled", config.isEnabled)
            .apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("pref_biometric_enabled", false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pref_biometric_enabled", enabled).apply()
    }

    fun getMlAccuracy(): Double {
        return prefs.getFloat("ml_accuracy", 88.5f).toDouble()
    }

    fun getMlCycles(): Int {
        return prefs.getInt("ml_cycles", 14)
    }

    fun getMlBestModel(): String {
        return prefs.getString("ml_best_model", "Monte Carlo (1k Runs)") ?: "Monte Carlo (1k Runs)"
    }

    fun saveMlMetrics(accuracy: Double, cycles: Int, bestModel: String) {
        prefs.edit()
            .putFloat("ml_accuracy", accuracy.toFloat())
            .putInt("ml_cycles", cycles)
            .putString("ml_best_model", bestModel)
            .apply()
    }

    fun getLastSyncTimestamp(): String {
        return prefs.getString("last_sync_timestamp", "Today 03:30 PM") ?: "Today 03:30 PM"
    }

    fun saveLastSyncTimestamp(timestamp: String) {
        prefs.edit().putString("last_sync_timestamp", timestamp).apply()
    }
}
