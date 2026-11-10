package com.example.multibaggerbot.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportHelper {

    fun generateAuditPdfReport(context: Context, positions: List<ActiveAuditPosition>) {
        if (positions.isEmpty()) {
            Toast.makeText(context, "No active audit positions to generate report.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val document = PdfDocument()
            val pageWidth = 595 // Standard A4 width in pt
            val pageHeight = 842 // Standard A4 height in pt
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

            // 1. Header Banner (Deep Navy)
            paint.color = Color.rgb(20, 26, 44)
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 70f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 16f
            canvas.drawText("MULTIBAGGER RESEARCH DOSSIER & AUDIT", 20f, 32f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f
            paint.color = Color.rgb(180, 200, 240)
            canvas.drawText("Stage-2 Trend Momentum • Fundamental Moat • 1–2 Year Horizon", 20f, 48f, paint)
            canvas.drawText("Generated: $dateStr", 20f, 60f, paint)

            // 2. Portfolio Summary Box
            val totalInitial = positions.sumOf { it.entryPrice }
            val totalCurrent = positions.sumOf { it.currentPrice }
            val netGain = totalCurrent - totalInitial
            val netGainPct = if (totalInitial > 0) (netGain / totalInitial) * 100.0 else 0.0

            paint.color = Color.rgb(240, 243, 250)
            canvas.drawRoundRect(20f, 80f, (pageWidth - 20).toFloat(), 135f, 8f, 8f, paint)

            paint.color = Color.rgb(40, 50, 70)
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("PORTFOLIO VALUATION SUMMARY", 30f, 96f, paint)

            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.BLACK
            canvas.drawText("Initial Outlay: INR ${String.format("%,.1f", totalInitial)}", 30f, 114f, paint)
            canvas.drawText("Live CMP: INR ${String.format("%,.1f", totalCurrent)}", 180f, 114f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = if (netGain >= 0) Color.rgb(30, 140, 50) else Color.rgb(200, 40, 40)
            canvas.drawText("Net Gain: ${if (netGain >= 0) "+" else ""}INR ${String.format("%,.1f", netGain)} (${String.format("%.2f", netGainPct)}%)", 330f, 114f, paint)

            val healthyCount = positions.count { it.healthStatus == "HEALTHY_COMPOUNDER" }
            val dumpCount = positions.count { it.healthStatus == "DUMP_WARNING" }
            paint.color = Color.DKGRAY
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Holdings: ${positions.size} Active | Healthy: $healthyCount | Dump Warnings: $dumpCount", 30f, 128f, paint)

            // 3. Table Headers
            var y = 155f
            paint.color = Color.rgb(230, 235, 245)
            canvas.drawRect(20f, y, (pageWidth - 20).toFloat(), y + 18f, paint)

            paint.color = Color.rgb(30, 40, 60)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8f
            canvas.drawText("TICKER", 25f, y + 12f, paint)
            canvas.drawText("SECTOR", 90f, y + 12f, paint)
            canvas.drawText("ENTRY", 170f, y + 12f, paint)
            canvas.drawText("CMP", 220f, y + 12f, paint)
            canvas.drawText("P&L (%)", 270f, y + 12f, paint)
            canvas.drawText("1Y 2X TGT", 330f, y + 12f, paint)
            canvas.drawText("STOP", 390f, y + 12f, paint)
            canvas.drawText("F-SCORE", 440f, y + 12f, paint)
            canvas.drawText("HEALTH", 495f, y + 12f, paint)

            y += 24f

            // 4. Position Rows
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            for (p in positions.take(22)) { // Fits up to 22 positions cleanly on page 1
                val isProfit = p.currentPrice >= p.entryPrice
                val isDump = p.healthStatus == "DUMP_WARNING"

                paint.color = Color.BLACK
                paint.textSize = 8f
                canvas.drawText(p.ticker.replace(".NS", ""), 25f, y, paint)

                paint.color = Color.DKGRAY
                canvas.drawText(p.sector.take(14), 90f, y, paint)

                paint.color = Color.BLACK
                canvas.drawText(String.format("%,.0f", p.entryPrice), 170f, y, paint)
                canvas.drawText(String.format("%,.0f", p.currentPrice), 220f, y, paint)

                paint.color = if (isProfit) Color.rgb(30, 130, 40) else Color.rgb(200, 30, 30)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${if (p.pnlPct >= 0) "+" else ""}${String.format("%.1f", p.pnlPct)}%", 270f, y, paint)

                paint.color = Color.rgb(30, 100, 180)
                canvas.drawText(String.format("%,.0f", p.target1Year2x), 330f, y, paint)

                paint.color = Color.rgb(180, 50, 50)
                canvas.drawText(String.format("%,.0f", p.stopLoss), 390f, y, paint)

                paint.color = Color.rgb(40, 60, 100)
                canvas.drawText("${p.piotroskiFScore}/9", 445f, y, paint)

                paint.color = if (isDump) Color.rgb(200, 30, 30) else Color.rgb(30, 120, 40)
                canvas.drawText(if (isDump) "DUMP EXIT" else "HEALTHY", 495f, y, paint)

                paint.color = Color.rgb(235, 235, 235)
                canvas.drawLine(20f, y + 4f, (pageWidth - 20).toFloat(), y + 4f, paint)

                y += 16f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            // 5. Quantitative Consensus Methodology Box
            y = 580f.coerceAtLeast(y + 10f)
            paint.color = Color.rgb(245, 247, 252)
            canvas.drawRoundRect(20f, y, (pageWidth - 20).toFloat(), y + 90f, 6f, 6f, paint)

            paint.color = Color.rgb(40, 60, 100)
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("QUANTITATIVE VALUATION METHODOLOGY & RISK MANAGEMENT", 30f, y + 16f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7.5f
            paint.color = Color.DKGRAY
            canvas.drawText("• Multi-Model Forecasting: Blends Discounted Cash Flow (DCF), Monte Carlo (10,000 runs), GBM, ARIMA, & Deep Learning.", 30f, y + 32f, paint)
            canvas.drawText("• Stan Weinstein Stage-2 Framework: Positions require upward-sloping 200-DMA & volume surge on 52W High breakout.", 30f, y + 46f, paint)
            canvas.drawText("• Strict Dump Protocol: Immediate exit when price closes below 200-DMA stop-loss floor to prevent capital drawdowns.", 30f, y + 60f, paint)
            canvas.drawText("• Fundamental Filter: Minimum 20% Sales CAGR, 25% PAT CAGR, Debt-to-Equity < 0.8, and zero promoter share pledge.", 30f, y + 74f, paint)

            // 6. Regulatory Disclaimer Footer
            paint.color = Color.GRAY
            paint.textSize = 7f
            canvas.drawText("⚠️ DISCLAIMER: For educational & research analytics only. Not an investment recommendation or SEBI advisory.", 20f, 810f, paint)
            canvas.drawText("Multibagger Bot v1.0.0 • Verified NSE Equity Ledger", 20f, 822f, paint)

            document.finishPage(page)

            // Save PDF to cache
            val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(context.cacheDir, "Multibagger_Report_$fileStamp.pdf")
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()

            // Open via Share / View Intent
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Multibagger Institutional Research Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(intent, "Open / Share Research PDF").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            Toast.makeText(context, "Research PDF Dossier Generated!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "PDF Generation error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
