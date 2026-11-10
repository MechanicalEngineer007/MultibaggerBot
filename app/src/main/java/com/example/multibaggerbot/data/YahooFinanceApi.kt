package com.example.multibaggerbot.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class YahooFinanceApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val requestCounter = AtomicInteger(0)

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0",
        "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    )

    private val hosts = listOf(
        "query1.finance.yahoo.com",
        "query2.finance.yahoo.com"
    )

    private fun getNextHost(): String {
        val idx = Math.abs(requestCounter.getAndIncrement()) % hosts.size
        return hosts[idx]
    }

    private fun getRandomUserAgent(): String {
        return userAgents.random()
    }

    /**
     * Rapidly fetch the real-time or latest regular market price for a single ticker.
     */
    suspend fun fetchCurrentPrice(ticker: String): Double? = withContext(Dispatchers.IO) {
        val clean = ticker.trim().uppercase()
        val sym = if (clean.endsWith(".NS") || clean.endsWith(".BO")) clean else "$clean.NS"
        val encoded = try { URLEncoder.encode(sym, "UTF-8") } catch (e: Exception) { sym }

        for (attempt in 0..1) {
            val host = if (attempt == 0) getNextHost() else "query1.finance.yahoo.com"
            val url = "https://$host/v8/finance/chart/$encoded?range=1d&interval=1d"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", getRandomUserAgent())
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.code == 429) {
                        // Rate limited -> pause and retry on alternative host
                        if (attempt == 0) {
                            delay(1500)
                            return@use // continue to attempt 1
                        }
                        return@withContext null
                    }
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    val parsed = json.decodeFromString<YahooChartResponse>(body)
                    val result = parsed.chart?.result?.firstOrNull() ?: return@withContext null
                    val price = result.meta?.regularMarketPrice ?: result.indicators?.quote?.firstOrNull()?.close?.filterNotNull()?.lastOrNull()
                    return@withContext price
                }
            } catch (e: Exception) {
                if (attempt == 0) {
                    delay(500)
                }
            }
        }
        null
    }

    /**
     * Concurrently fetch live prices for multiple stocks.
     */
    suspend fun fetchCurrentPrices(tickers: List<String>): Map<String, Double> = withContext(Dispatchers.IO) {
        val results = tickers.map { t ->
            async {
                val p = fetchCurrentPrice(t)
                if (p != null) t to p else null
            }
        }.awaitAll().filterNotNull().toMap()
        results
    }

    /**
     * Fetch historical daily OHLCV candles for technical, Stage-2, and momentum scoring.
     */
    suspend fun fetchHistoricalData(ticker: String, range: String = "1y", interval: String = "1d"): StockData? = withContext(Dispatchers.IO) {
        val clean = ticker.trim().uppercase()
        val sym = if (clean.endsWith(".NS") || clean.endsWith(".BO")) clean else "$clean.NS"

        val encoded = try {
            URLEncoder.encode(sym, "UTF-8")
        } catch (e: Exception) {
            sym
        }

        for (attempt in 0..1) {
            val host = if (attempt == 0) getNextHost() else (if (hosts[0] != getNextHost()) hosts[0] else hosts[1])
            val url = "https://$host/v8/finance/chart/$encoded?range=$range&interval=$interval"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", getRandomUserAgent())
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.code == 429) {
                        // Rate limit detected, back off and retry once
                        if (attempt == 0) {
                            delay(1800)
                            return@use
                        }
                        return@withContext null
                    }
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null

                    val parsed = json.decodeFromString<YahooChartResponse>(body)
                    val result = parsed.chart?.result?.firstOrNull() ?: return@withContext null
                    val meta = result.meta

                    val timestamps = result.timestamp ?: return@withContext null
                    val quote = result.indicators?.quote?.firstOrNull() ?: return@withContext null

                    val candles = mutableListOf<Candle>()
                    for (i in timestamps.indices) {
                        val open = quote.open?.getOrNull(i)
                        val high = quote.high?.getOrNull(i)
                        val low = quote.low?.getOrNull(i)
                        val close = quote.close?.getOrNull(i)
                        val volume = quote.volume?.getOrNull(i) ?: 1000L

                        if (open != null && high != null && low != null && close != null) {
                            candles.add(Candle(timestamps[i], open, high, low, close, volume))
                        }
                    }

                    if (candles.size >= 40) {
                        return@withContext StockData(sym, candles, meta)
                    }
                }
            } catch (e: Exception) {
                if (attempt == 0) {
                    delay(600)
                }
            }
        }
        return@withContext null
    }
}
