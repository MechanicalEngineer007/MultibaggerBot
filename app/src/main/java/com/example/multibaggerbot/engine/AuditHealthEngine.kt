package com.example.multibaggerbot.engine

data class HealthAuditEvaluation(
    val status: String, // "HEALTHY_COMPOUNDER", "WATCH_CAUTION", "DUMP_WARNING"
    val warnings: List<String>
)

object AuditHealthEngine {

    fun evaluate(
        livePrice: Double,
        entryPrice: Double,
        stopLoss: Double,
        pnlPct: Double,
        altmanZScore: Double,
        piotroskiFScore: Int,
        existingWarnings: List<String> = emptyList()
    ): HealthAuditEvaluation {
        val warnings = mutableListOf<String>()

        val isBelowStop = stopLoss > 0 && livePrice < stopLoss
        val distToStopPct = if (stopLoss > 0 && livePrice >= stopLoss) {
            ((livePrice - stopLoss) / livePrice) * 100.0
        } else 999.0
        val isNearStop = distToStopPct <= 5.0 // within 5% proximity of 200-DMA stop floor

        // Drawdown thresholds
        val isSevereDrawdown = pnlPct <= -10.0
        val isModerateDrawdown = pnlPct < -3.5 && !isSevereDrawdown

        // Altman Z-Score:
        // Distress: < 1.8 (High bankruptcy/insolvency risk)
        // Grey Zone: 1.8 to 2.85
        val isAltmanDistress = altmanZScore in 0.1..1.79
        val isAltmanGrey = altmanZScore in 1.80..2.85

        // Piotroski F-Score:
        // Collapse: <= 4 (severe fundamental decay)
        // Borderline: 5
        val isPiotroskiCollapse = piotroskiFScore in 1..4
        val isPiotroskiBorderline = piotroskiFScore == 5

        // Check for Stage-4 breakdown in historical records
        val hasStage4Warning = existingWarnings.any {
            it.contains("Stage-4", ignoreCase = true) || it.contains("Breakdown", ignoreCase = true)
        }

        // 1. DUMP / EXIT WARNING TRIGGERS (🚨)
        if (isBelowStop) {
            warnings.add("🚨 Breached 200-DMA Stop Floor (CMP ₹${String.format("%,.1f", livePrice)} < Stop ₹${String.format("%,.1f", stopLoss)})")
        }
        if (isSevereDrawdown) {
            warnings.add("🚨 Drawdown Floor Breached: P&L dropped to ${String.format("%.1f", pnlPct)}% (Max Limit: -10%)")
        }
        if (hasStage4Warning) {
            warnings.add("🚨 Stan Weinstein Stage-4 Breakdown: Closed below 200-DMA with declining momentum")
        }
        if (isAltmanDistress) {
            warnings.add("🚨 Altman Z Distress Zone (${String.format("%.1f", altmanZScore)} < 1.80 — High Default Risk)")
        }
        if (isPiotroskiCollapse) {
            warnings.add("🚨 Piotroski Score Collapse (${piotroskiFScore}/9 — Severe fundamental deterioration)")
        }

        // 2. WATCH / CAUTION WARNING TRIGGERS (⚠️)
        if (isNearStop && !isBelowStop) {
            warnings.add("⚠️ Stop Floor Danger: CMP is within ${String.format("%.1f", distToStopPct)}% of Stop Floor (₹${String.format("%,.1f", stopLoss)})")
        }
        if (isModerateDrawdown && !isSevereDrawdown) {
            warnings.add("⚠️ Capital Drawdown Alert: Position is down ${String.format("%.1f", pnlPct)}% from entry")
        }
        if (isAltmanGrey && !isAltmanDistress) {
            warnings.add("⚠️ Altman Z Grey Zone (${String.format("%.1f", altmanZScore)} — Fragile Balance Sheet & Debt)")
        }
        if (isPiotroskiBorderline && !isPiotroskiCollapse) {
            warnings.add("⚠️ Borderline Piotroski Score (5/9 — Decelerating operating margins)")
        }

        // Preserve any custom user-added notes/warnings from existingWarnings
        for (ew in existingWarnings) {
            if (warnings.none { it.contains(ew.take(15)) || ew.contains(it.take(15)) }) {
                warnings.add(ew)
            }
        }

        val isDump = isBelowStop || isSevereDrawdown || hasStage4Warning || isAltmanDistress || isPiotroskiCollapse
        val isCaution = !isDump && (warnings.isNotEmpty() || isNearStop || isModerateDrawdown || isAltmanGrey || isPiotroskiBorderline)

        val finalStatus = when {
            isDump -> "DUMP_WARNING"
            isCaution -> "WATCH_CAUTION"
            else -> "HEALTHY_COMPOUNDER"
        }

        return HealthAuditEvaluation(finalStatus, warnings)
    }
}
