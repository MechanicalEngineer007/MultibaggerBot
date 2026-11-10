package com.example.multibaggerbot.data

import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExportHelper {

    fun exportAuditPositions(context: Context, positions: List<ActiveAuditPosition>) {
        if (positions.isEmpty()) {
            Toast.makeText(context, "No active audit positions to export.", Toast.LENGTH_SHORT).show()
            return
        }

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Multibagger_Live_Audit_$dateStr.csv"

        val sb = StringBuilder()
        sb.append("Ticker,Company Name,Sector,Entry Price (INR),Current Price (INR),P&L (INR),P&L (%),Health Status,1Y Target (INR),200-DMA Stop (INR),DCF 1Y FV,Monte Carlo Median,Consensus Target,Dump Warnings\n")

        for (p in positions) {
            val pnlRupees = p.currentPrice - p.entryPrice
            val warnings = p.dumpWarnings.joinToString(" | ").replace(",", ";")
            val cleanName = p.companyName.replace(",", " ")
            val cleanSector = p.sector.replace(",", " ")

            sb.append("${p.ticker},")
            sb.append("\"$cleanName\",")
            sb.append("\"$cleanSector\",")
            sb.append("${p.entryPrice},")
            sb.append("${p.currentPrice},")
            sb.append("${String.format("%.2f", pnlRupees)},")
            sb.append("${p.pnlPct}%,")
            sb.append("${p.healthStatus},")
            sb.append("${p.target1Year2x},")
            sb.append("${p.stopLoss},")
            sb.append("${p.dcf1YrFV},")
            sb.append("${p.mcMedian},")
            sb.append("${p.quantConsensus},")
            sb.append("\"$warnings\"\n")
        }

        shareCsvContent(context, fileName, sb.toString(), "Multibagger Live Audit Ledger ($dateStr)")
    }

    fun exportSimulationConfig(context: Context, config: SimulationConfig) {
        if (config.stocks.isEmpty()) {
            Toast.makeText(context, "No stocks in this simulation configuration to export.", Toast.LENGTH_SHORT).show()
            return
        }

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanConfigName = config.name.replace(" ", "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
        val fileName = "Simulation_${cleanConfigName}_$dateStr.csv"

        val sb = StringBuilder()
        sb.append("Simulation Configuration: ${config.name}\n")
        sb.append("Initial Capital: INR ${config.initialCapital}, Target Return: +${config.targetReturnPct}%, Horizon: ${config.timeHorizonMonths} Months, Stop Floor: -${config.trailingStopLossPct}%\n\n")
        sb.append("Ticker,Company Name,Shares,Initial Price (INR),Current Price (INR),Initial Invested (INR),Current Value (INR),Net Gain (INR),Net Gain (%),Predicted Value (INR)\n")

        for (s in config.stocks) {
            val initialVal = s.shares * s.initialPrice
            val currentVal = s.shares * s.currentPrice
            val netGain = currentVal - initialVal
            val gainPct = if (initialVal > 0) (netGain / initialVal) * 100.0 else 0.0
            val predictedVal = initialVal * (1.0 + config.targetReturnPct / 100.0)

            sb.append("${s.symbol},")
            sb.append("\"${s.companyName.replace(",", " ")}\",")
            sb.append("${s.shares},")
            sb.append("${s.initialPrice},")
            sb.append("${s.currentPrice},")
            sb.append("${String.format("%.1f", initialVal)},")
            sb.append("${String.format("%.1f", currentVal)},")
            sb.append("${String.format("%.1f", netGain)},")
            sb.append("${String.format("%.2f", gainPct)}%,")
            sb.append("${String.format("%.1f", predictedVal)}\n")
        }

        shareCsvContent(context, fileName, sb.toString(), "Multibagger Simulation Portfolio: ${config.name}")
    }

    private fun shareCsvContent(context: Context, fileName: String, content: String, title: String) {
        try {
            // Write to app-specific external files dir
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, fileName)
            file.writeText(content)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, content)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(shareIntent, "Export & Share Portfolio CSV").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            Toast.makeText(context, "Exported: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
