package com.example.multibaggerbot.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.multibaggerbot.MainActivity
import com.example.multibaggerbot.R
import com.example.multibaggerbot.data.ActiveAuditPosition
import com.example.multibaggerbot.data.MultibaggerRepository

class MultibaggerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, MultibaggerWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (id in appWidgetIds) {
                        updateWidgetView(context, appWidgetManager, id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun updateWidgetView(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_multibagger)
            val repository = MultibaggerRepository(context)
            val positions: List<ActiveAuditPosition> = repository.getAuditPositionsSync()

            val totalInitial = positions.sumOf { it.entryPrice }
            val totalCurrent = positions.sumOf { it.currentPrice }
            val netGain = totalCurrent - totalInitial
            val netGainPct = if (totalInitial > 0.0) (netGain / totalInitial) * 100.0 else 0.0

            val dumpCount = positions.count { it.healthStatus == "DUMP_WARNING" }
            val cautionCount = positions.count { it.healthStatus == "WATCH_CAUTION" }
            val isDump = dumpCount > 0
            val isCaution = cautionCount > 0

            // 1. Portfolio Valuation
            if (positions.isNotEmpty()) {
                views.setTextViewText(R.id.widget_portfolio_val, "₹${String.format("%,.0f", totalCurrent)}")
                val gainSign = if (netGain >= 0.0) "+" else ""
                views.setTextViewText(
                    R.id.widget_portfolio_gain,
                    "${gainSign}₹${String.format("%,.0f", netGain)} (${gainSign}${String.format("%.1f", netGainPct)}%)"
                )
                views.setTextColor(
                    R.id.widget_portfolio_gain,
                    if (netGain >= 0.0) Color.rgb(76, 175, 80) else Color.rgb(239, 83, 80)
                )
            } else {
                views.setTextViewText(R.id.widget_portfolio_val, "₹0.0")
                views.setTextViewText(R.id.widget_portfolio_gain, "Tap to Scan")
            }

            // 2. Health & Dump Status
            if (isDump) {
                views.setTextViewText(R.id.widget_health_status, "🚨 $dumpCount DUMP WARNINGS")
                views.setTextColor(R.id.widget_health_status, Color.rgb(255, 82, 82))
            } else if (isCaution) {
                views.setTextViewText(R.id.widget_health_status, "⚠️ $cautionCount CAUTION ALERTS")
                views.setTextColor(R.id.widget_health_status, Color.rgb(255, 183, 77))
            } else {
                views.setTextViewText(R.id.widget_health_status, "✅ HEALTHY (${positions.size} HOLDINGS)")
                views.setTextColor(R.id.widget_health_status, Color.rgb(76, 175, 80))
            }

            // 3. Top Breakout Spotlight
            val topPick = positions.maxByOrNull { it.pnlPct }
            if (topPick != null) {
                views.setTextViewText(
                    R.id.widget_breakout_text,
                    "🎯 Top: ${topPick.ticker.replace(".NS", "")} • CMP: ₹${String.format("%,.0f", topPick.currentPrice)} • Tgt: ₹${String.format("%,.0f", topPick.target1Year2x)}"
                )
            } else {
                views.setTextViewText(R.id.widget_breakout_text, "🎯 Radar active across 2,598 NSE Equities")
            }

            // 4. Click Intent: Opens MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
