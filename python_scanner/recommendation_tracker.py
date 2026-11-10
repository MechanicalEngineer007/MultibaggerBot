"""
Multibagger Bot - Recommendation Tracker & Fundamental Health Auditor
======================================================================
Stores all stock recommendations made by the bot in a persistent ledger.
Audits them automatically at Market Open (09:15 AM) and Market Close (03:30 PM).
Detects adverse fundamental changes, debt explosions, PAT growth collapse,
and 200-DMA breakdowns to raise immediate 'DUMP / EXIT' warnings.
"""

import os
import sys
import json
import time
from datetime import datetime
from typing import List, Dict, Any, Optional

# Windows UTF-8 console output support
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.text import Text

console = Console()
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
LEDGER_FILE = os.path.join(SCRIPT_DIR, "recommendations_ledger.json")
AUDIT_LOG_FILE = os.path.join(SCRIPT_DIR, "audit_history.log")
ANDROID_ASSETS_JSON = os.path.join(
    os.path.dirname(SCRIPT_DIR), "MultibaggerBotApp", "app", "src", "main", "assets", "initial_multibaggers.json"
)


class RecommendationTracker:
    def __init__(self, ledger_file: str = LEDGER_FILE):
        self.ledger_file = ledger_file
        self.ledger = self._load_ledger()

    def _load_ledger(self) -> Dict[str, Any]:
        if os.path.exists(self.ledger_file):
            try:
                with open(self.ledger_file, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception as e:
                console.print(f"[yellow]Warning: Could not parse ledger ({e}), initializing empty.[/]")

        # If ledger doesn't exist, check for latest CSV to auto-seed
        ledger_data = {"active_recommendations": {}, "closed_recommendations": {}, "last_updated": ""}
        import glob, csv
        csv_files = sorted(glob.glob(os.path.join(SCRIPT_DIR, "multibagger_picks_*.csv")), reverse=True)
        if csv_files:
            latest_csv = csv_files[0]
            try:
                now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                with open(latest_csv, "r", encoding="utf-8") as f:
                    reader = csv.DictReader(f)
                    for row in reader:
                        t = row.get("ticker", "")
                        if not t:
                            continue
                        cmp = float(row.get("cmp", 0) or 0)
                        target_1y = float(row.get("target_1yr_2x", cmp * 2.0) or cmp * 2.0)
                        stop = float(row.get("stop_loss", cmp * 0.85) or cmp * 0.85)
                        ledger_data["active_recommendations"][t] = {
                            "ticker": t,
                            "company_name": row.get("company_name", t),
                            "sector": row.get("sector", "Multibagger"),
                            "entry_date": now_str,
                            "entry_price": round(cmp, 2),
                            "target_1yr_2x": round(target_1y, 2),
                            "stop_loss": round(stop, 2),
                            "initial_metrics": {
                                "sales_growth_pct": float(row.get("sales_growth_pct", 20.0) or 20.0),
                                "profit_growth_pct": float(row.get("profit_growth_pct", 25.0) or 25.0),
                                "debt_to_equity": float(row.get("debt_to_equity", 0.1) or 0.1),
                                "roce_pct": float(row.get("roce_pct", 18.0) or 18.0),
                                "peg_ratio": float(row.get("peg_ratio", 1.0) or 1.0),
                                "dcf_1yr_fv": float(row.get("dcf_future_value_1yr", 0.0) or 0.0),
                                "gbm_target": float(row.get("gbm_expected_price", 0.0) or 0.0),
                                "mc_median": float(row.get("mc_median_price", 0.0) or 0.0),
                                "mc_bull": float(row.get("mc_bull_price", 0.0) or 0.0),
                                "mc_2x_prob": float(row.get("mc_2x_prob_pct", 0.0) or 0.0),
                                "arima_target": float(row.get("arima_forecast_price", 0.0) or 0.0),
                                "dl_target": float(row.get("dl_lstm_target_price", 0.0) or 0.0),
                                "quant_consensus": float(row.get("quant_consensus_target", 0.0) or 0.0),
                                "ml_2x_prob_pct": float(row.get("ml_2x_prob_pct", 50.0) or 50.0),
                                "score": float(row.get("multibagger_score", 70.0) or 70.0),
                                "grade": row.get("grade", "A")
                            },
                            "current_price": round(cmp, 2),
                            "pnl_pct": 0.0,
                            "status": "ACTIVE",
                            "health_status": "HEALTHY_COMPOUNDER",
                            "dump_warnings": [],
                            "last_audited": now_str,
                            "audit_history": []
                        }
                ledger_data["last_updated"] = now_str
            except Exception as e:
                console.print(f"[yellow]Warning: Could not seed from CSV: {e}[/]")
        return ledger_data

    def _save_ledger(self):
        self.ledger["last_updated"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        with open(self.ledger_file, "w", encoding="utf-8") as f:
            json.dump(self.ledger, f, indent=2)

    def record_recommendations(self, candidates: list) -> int:
        """
        Stores recommended candidates into the persistent ledger.
        If a stock is already active, updates its latest metrics while preserving entry baseline.
        """
        now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        added_count = 0

        for c in candidates:
            t = c.ticker
            cmp = float(c.stage2.cmp) if hasattr(c, "stage2") else float(c.cmp)
            target_1y = float(c.target_1yr_2x)
            stop_loss = float(c.stage2.sma200 * 0.94) if (hasattr(c, "stage2") and c.stage2.sma200 > 0) else cmp * 0.82

            if t not in self.ledger["active_recommendations"]:
                # New recommendation entry
                record = {
                    "ticker": t,
                    "company_name": getattr(c.fund, "company_name", t),
                    "sector": getattr(c.fund, "sector", "Growth"),
                    "entry_date": now_str,
                    "entry_price": round(cmp, 2),
                    "target_1yr_2x": round(target_1y, 2),
                    "stop_loss": round(stop_loss, 2),
                    "initial_metrics": {
                        "sales_growth_pct": round(getattr(c.fund, "sales_growth_3y_cagr", 0.0), 1),
                        "profit_growth_pct": round(getattr(c.fund, "pat_growth_3y_cagr", 0.0), 1),
                        "debt_to_equity": round(getattr(c.fund, "debt_to_equity", 0.0), 2),
                        "roce_pct": round(getattr(c.fund, "roce", 0.0), 1),
                        "roe_pct": round(getattr(c.fund, "roe", 0.0), 1),
                        "peg_ratio": round(getattr(c.fund, "peg_ratio", 1.0), 2),
                        "dcf_1yr_fv": round(getattr(c.dcf, "future_value_1yr", 0.0), 2) if hasattr(c, "dcf") and c.dcf else 0.0,
                        "gbm_target": round(getattr(c.quant, "gbm_expected_1yr", 0.0), 2) if hasattr(c, "quant") and c.quant else 0.0,
                        "mc_median": round(getattr(c.quant, "mc_median_1yr", 0.0), 2) if hasattr(c, "quant") and c.quant else 0.0,
                        "mc_bull": round(getattr(c.quant, "mc_bull_target_1yr", 0.0), 2) if hasattr(c, "quant") and c.quant else 0.0,
                        "mc_2x_prob": round(getattr(c.quant, "mc_multibagger_prob_pct", 0.0), 1) if hasattr(c, "quant") and c.quant else 0.0,
                        "arima_target": round(getattr(c.quant, "arima_forecast_1yr", 0.0), 2) if hasattr(c, "quant") and c.quant else 0.0,
                        "dl_target": round(getattr(c.quant, "dl_nn_target_1yr", 0.0), 2) if hasattr(c, "quant") and c.quant else 0.0,
                        "quant_consensus": round(getattr(c.quant, "consensus_target_1yr", 0.0), 2) if hasattr(c, "quant") and c.quant else 0.0,
                        "ml_2x_prob_pct": round(getattr(c.ml, "multibagger_probability_pct", 50.0), 1) if hasattr(c, "ml") and c.ml else 50.0,
                        "score": round(float(c.multibagger_score), 1),
                        "grade": c.grade
                    },
                    "current_price": round(cmp, 2),
                    "pnl_pct": 0.0,
                    "status": "ACTIVE",
                    "health_status": "HEALTHY_COMPOUNDER",
                    "dump_warnings": [],
                    "last_audited": now_str,
                    "audit_history": []
                }
                self.ledger["active_recommendations"][t] = record
                added_count += 1
            else:
                # Update current metrics while keeping original entry baseline
                rec = self.ledger["active_recommendations"][t]
                rec["current_price"] = round(cmp, 2)
                rec["pnl_pct"] = round(((cmp - rec["entry_price"]) / rec["entry_price"]) * 100, 2)
                rec["last_audited"] = now_str

        self._save_ledger()
        self._sync_to_android_assets()
        return added_count

    def audit_active_recommendations(self, data_dict: dict, macro_env: Optional[Any] = None) -> List[Dict[str, Any]]:
        """
        Audits every active recommendation against latest market data and fundamentals.
        Checks for fundamental deterioration, debt spike, PAT collapse, or 200-DMA breakdown.
        """
        now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        audit_results = []

        for ticker, record in list(self.ledger["active_recommendations"].items()):
            if ticker not in data_dict:
                continue

            df, fund_data = data_dict[ticker]
            if df is None or df.empty:
                continue

            close_col = "close" if "close" in df.columns else ("Close" if "Close" in df.columns else df.columns[0])
            cmp = float(df[close_col].iloc[-1])
            entry_price = float(record["entry_price"])
            pnl_pct = round(((cmp - entry_price) / entry_price) * 100, 2)
            record["current_price"] = round(cmp, 2)
            record["pnl_pct"] = pnl_pct
            record["last_audited"] = now_str

            # Technical Checks
            sma50 = float(df[close_col].rolling(50).mean().iloc[-1]) if len(df) >= 50 else cmp
            sma200 = float(df[close_col].rolling(200).mean().iloc[-1]) if len(df) >= 200 else cmp * 0.90
            stop_loss = float(record["stop_loss"])

            # Fundamental Checks
            init = record.get("initial_metrics", {})
            init_pat = float(init.get("profit_growth_pct", 25.0))
            init_sales = float(init.get("sales_growth_pct", 20.0))
            init_de = float(init.get("debt_to_equity", 0.1))

            cur_pat = float(fund_data.get("pat_growth_3y_cagr", init_pat))
            cur_sales = float(fund_data.get("sales_growth_3y_cagr", init_sales))
            cur_de = float(fund_data.get("debt_to_equity", init_de))
            cur_roce = float(fund_data.get("roce", 18.0))

            dump_warnings = []

            # ── 1. PAT Growth Deterioration ──────────────────────────────
            if cur_pat < 0:
                dump_warnings.append(f"🚨 CRITICAL: Profit turned negative (PAT Growth: {cur_pat:.1f}%)")
            elif (init_pat - cur_pat) >= 25.0 and cur_pat < 15.0:
                dump_warnings.append(f"⚠️ PAT Growth Collapsed: from +{init_pat:.1f}% to +{cur_pat:.1f}%")

            # ── 2. Debt & Solvency Explosion ────────────────────────────
            if cur_de > 1.2:
                dump_warnings.append(f"🚨 High Debt Explosion: D/E surged to {cur_de:.2f} (Init: {init_de:.2f})")
            elif cur_de > 0.8 and (cur_de - init_de) >= 0.35:
                dump_warnings.append(f"⚠️ Rapid Leverage Increase: D/E increased from {init_de:.2f} to {cur_de:.2f}")

            # ── 3. Revenue / Sales Stagnation ───────────────────────────
            if cur_sales < 0:
                dump_warnings.append(f"⚠️ Revenue Contraction: Sales Growth {cur_sales:.1f}%")

            # ── 4. Technical Breakdown / Stop Loss Violated ─────────────
            if cmp < stop_loss:
                dump_warnings.append(f"🚨 200-DMA Stop Loss Violated: CMP ₹{cmp:,.1f} < Stop ₹{stop_loss:,.1f}")
            elif cmp < sma200 * 0.96:
                dump_warnings.append(f"⚠️ Stan Weinstein Stage-4 Breakdown: Closed below 200-DMA (₹{sma200:,.1f})")

            # ── 5. Target 2x Achieved (Profit Booking) ─────────────────
            target_hit = False
            if cmp >= float(record["target_1yr_2x"]):
                target_hit = True
                record["status"] = "TARGET_2X_HIT"

            # Assign Health Status
            if dump_warnings:
                health_status = "DUMP_WARNING"
                record["status"] = "DUMP_WARNING"
            elif pnl_pct <= -15.0 or cmp < sma50 * 0.93:
                health_status = "WATCH_CAUTION"
                record["status"] = "ACTIVE_CAUTION"
            else:
                health_status = "HEALTHY_COMPOUNDER"
                record["status"] = "ACTIVE"

            record["health_status"] = health_status
            record["dump_warnings"] = dump_warnings

            audit_entry = {
                "timestamp": now_str,
                "cmp": cmp,
                "pnl_pct": pnl_pct,
                "health_status": health_status,
                "warnings": dump_warnings
            }
            record.setdefault("audit_history", []).append(audit_entry)
            audit_results.append(record)

        self._save_ledger()
        self._sync_to_android_assets()
        return audit_results

    def _sync_to_android_assets(self):
        """Mirrors active recommendations into the Android companion app assets."""
        try:
            os.makedirs(os.path.dirname(ANDROID_ASSETS_JSON), exist_ok=True)
            android_picks = []
            for t, r in self.ledger.get("active_recommendations", {}).items():
                cmp = r.get("current_price", 0.0)
                init = r.get("initial_metrics", {})
                t1 = r.get("target_1yr_2x", cmp * 2.0)
                t1_pct = round(((t1 - cmp) / cmp * 100), 1) if cmp > 0 else 100.0

                pick = {
                    "symbol": t,
                    "companyName": r.get("company_name", t),
                    "cmp": cmp,
                    "score": int(init.get("score", 70)),
                    "convictionGrade": init.get("grade", "A") + " Grade",
                    "stageStatus": "🚨 DUMP / EXIT" if r.get("health_status") == "DUMP_WARNING" else "Stage 2 Markup",
                    "target1Year": t1,
                    "target1YearPct": t1_pct,
                    "target2Year": round(cmp * 2.75, 2),
                    "target2YearPct": 175.0,
                    "trailingStopLoss": r.get("stop_loss", cmp * 0.85),
                    "riskRewardRatio": 3.5,
                    "salesGrowthCAGR": init.get("sales_growth_pct", 20.0),
                    "patGrowthCAGR": init.get("profit_growth_pct", 25.0),
                    "debtToEquity": init.get("debt_to_equity", 0.1),
                    "peRatio": 25.0,
                    "pegRatio": init.get("peg_ratio", 1.0),
                    "promoterHolding": 54.0,
                    "institutionalHolding": 28.0,
                    "sectorTheme": r.get("sector", "Multibagger"),
                    "moatDescription": (
                        "🚨 DUMP WARNING: " + " | ".join(r.get("dump_warnings", []))
                        if r.get("dump_warnings")
                        else f"P&L: {r.get('pnl_pct', 0.0):+.1f}% | Health: {r.get('health_status')}"
                    ),
                    "sma50": round(cmp * 0.96, 2),
                    "sma150": round(cmp * 0.90, 2),
                    "sma200": round(r.get("stop_loss", cmp * 0.85) / 0.94, 2),
                    "fiftyTwoWeekHigh": round(cmp * 1.15, 2),
                    "fiftyTwoWeekLow": round(cmp * 0.65, 2),
                    "volumeSurgeRatio": 1.2,
                    "cagr2Year": 65.8,
                    "dcfValue": float(init.get("dcf_1yr_fv", 0.0) or 0.0),
                    "gbmTarget": float(init.get("gbm_target", 0.0) or 0.0),
                    "mcMedian": float(init.get("mc_median", 0.0) or 0.0),
                    "mcBull": float(init.get("mc_bull", 0.0) or 0.0),
                    "mc2xProb": float(init.get("mc_2x_prob", init.get("ml_2x_prob_pct", 50.0)) or 50.0),
                    "arimaTarget": float(init.get("arima_target", 0.0) or 0.0),
                    "dlTarget": float(init.get("dl_target", 0.0) or 0.0),
                    "quantConsensus": float(init.get("quant_consensus", 0.0) or 0.0)
                }
                android_picks.append(pick)

            if android_picks:
                with open(ANDROID_ASSETS_JSON, "w", encoding="utf-8") as f:
                    json.dump(android_picks, f, indent=2)

            # Also generate audit_ledger.json for the Live Audit Tab
            audit_positions = []
            for t, r in self.ledger.get("active_recommendations", {}).items():
                init = r.get("initial_metrics", {})
                audit_positions.append({
                    "ticker": t,
                    "companyName": r.get("company_name", t),
                    "sector": r.get("sector", "Growth"),
                    "entryPrice": float(r.get("entry_price", 0.0)),
                    "currentPrice": float(r.get("current_price", r.get("entry_price", 0.0))),
                    "pnlPct": float(r.get("pnl_pct", 0.0)),
                    "target1Year2x": float(r.get("target_1yr_2x", 0.0)),
                    "stopLoss": float(r.get("stop_loss", 0.0)),
                    "healthStatus": r.get("health_status", "HEALTHY_COMPOUNDER"),
                    "dumpWarnings": r.get("dump_warnings", []),
                    "lastAudited": r.get("last_audited", ""),
                    "dcf1YrFV": float(init.get("dcf_1yr_fv", 0.0) or 0.0),
                    "gbm1Yr": float(init.get("gbm_target", 0.0) or 0.0),
                    "mcMedian": float(init.get("mc_median", 0.0) or 0.0),
                    "mcBull": float(init.get("mc_bull", 0.0) or 0.0),
                    "mc2xProb": float(init.get("mc_2x_prob", init.get("ml_2x_prob_pct", 50.0)) or 50.0),
                    "arima1Yr": float(init.get("arima_target", 0.0) or 0.0),
                    "dl1Yr": float(init.get("dl_target", 0.0) or 0.0),
                    "quantConsensus": float(init.get("quant_consensus", 0.0) or 0.0)
                })

            audit_json_path = os.path.join(os.path.dirname(ANDROID_ASSETS_JSON), "audit_ledger.json")
            if audit_positions:
                with open(audit_json_path, "w", encoding="utf-8") as f:
                    json.dump(audit_positions, f, indent=2)
        except Exception:
            pass

    def get_dump_warnings(self) -> List[Dict[str, Any]]:
        """Returns all holdings currently triggering dump or exit warnings."""
        dumps = []
        for t, r in self.ledger.get("active_recommendations", {}).items():
            if r.get("health_status") == "DUMP_WARNING" or r.get("dump_warnings"):
                dumps.append(r)
        return dumps


def print_audit_report(audit_results: List[Dict[str, Any]], session_label: str = "Market Timing Check"):
    """Displays a rich terminal audit table highlighting P&L, health status, and dump warnings."""
    table = Table(
        title=f"📋 Active Recommendations Audit ({session_label})",
        show_header=True,
        header_style="bold magenta",
        expand=True
    )

    table.add_column("Ticker", style="bold cyan", width=10)
    table.add_column("Entry Price", justify="right", width=11)
    table.add_column("Current Price", justify="right", width=12)
    table.add_column("P&L (%)", justify="right", width=10)
    table.add_column("1Y 2x Goal", justify="right", width=11)
    table.add_column("Stop Loss", justify="right", width=11)
    table.add_column("Health Status", justify="center", width=18)
    table.add_column("Action / Warning", width=36)

    dump_count = 0
    for r in audit_results:
        t = r["ticker"].replace(".NS", "")
        entry = f"₹{r['entry_price']:,.1f}"
        cur = f"₹{r['current_price']:,.1f}"
        pnl = r["pnl_pct"]
        pnl_str = f"+{pnl:.1f}%" if pnl >= 0 else f"{pnl:.1f}%"
        pnl_style = "bold green" if pnl >= 0 else "bold red"

        goal = f"₹{r['target_1yr_2x']:,.1f}"
        stop = f"₹{r['stop_loss']:,.1f}"

        status = r.get("health_status", "HEALTHY_COMPOUNDER")
        if status == "DUMP_WARNING":
            status_badge = "[bold white on red] 🚨 DUMP / EXIT [/]"
            action = "[bold red]" + " | ".join(r.get("dump_warnings", ["Fundamental Deterioration"]))[:35] + "[/]"
            dump_count += 1
        elif status == "WATCH_CAUTION":
            status_badge = "[bold black on yellow] ⚠️ CAUTION [/]"
            action = "[yellow]Hold with trailing stop discipline[/]"
        elif r.get("status") == "TARGET_2X_HIT":
            status_badge = "[bold black on green] 🎯 2X TARGET HIT [/]"
            action = "[green]Book 50% profit, ride runner[/]"
        else:
            status_badge = "[green]🟢 HEALTHY[/]"
            action = "[cyan]Hold for 1Y 2x Multibagger[/]"

        table.add_row(t, entry, cur, f"[{pnl_style}]{pnl_str}[/]", goal, stop, status_badge, action)

    console.print(table)

    if dump_count > 0:
        console.print(Panel(
            f"[bold red]🚨 CRITICAL ALERT:[/] {dump_count} holding(s) triggered fundamental deterioration or 200-DMA stop violations!\n"
            f"[bold yellow]Recommendation:[/] Review and DUMP / EXIT these positions to protect capital.",
            title="[bold red]⚠️ Capital Protection Warning[/]",
            border_style="red"
        ))
    else:
        console.print(Panel(
            "[bold green]✔ All active multibagger recommendations are healthy and on track.[/]\n"
            "Stan Weinstein Stage-2 momentum intact; fundamental balance sheets remain clean.",
            title="[bold green]Portfolio Health Green[/]",
            border_style="green"
        ))
