"""
Report Generator for Multibagger Bot.
Produces rich, color-coded terminal tables, deep-dive stock cards,
a 1-Year wealth projection simulator, and CSV export.
"""

import os
import csv
from datetime import datetime
from typing import List, Optional

from rich.console import Console

from rich.table import Table
from rich.panel import Panel
from rich.text import Text
from rich import box

from scorer import MultibaggerCandidate
from config import Config, DEFAULT_CONFIG
from macro_regime import MacroEnvironment

console = Console()



def _format_price(price: float) -> str:
    """Format price in Indian style (₹X,XXX.XX)."""
    if price >= 10000:
        return f"₹{price:,.0f}"
    elif price >= 100:
        return f"₹{price:,.1f}"
    else:
        return f"₹{price:,.2f}"


def _grade_color(grade: str) -> str:
    """Return color for multibagger grade."""
    if grade == "AAA":
        return "bold bright_green"
    elif grade == "AA":
        return "bold green"
    elif grade == "A":
        return "bold yellow"
    else:
        return "orange3"


def _score_color(score: float) -> str:
    """Return color for score."""
    if score >= 80:
        return "bold bright_green"
    elif score >= 70:
        return "bold green"
    elif score >= 55:
        return "yellow"
    else:
        return "orange3"


def print_multibagger_report(
    candidates: List[MultibaggerCandidate],
    config: Config = DEFAULT_CONFIG,
    macro_env: Optional[MacroEnvironment] = None
):
    """
    Print rich terminal console report including Global Macro Liquidity Dashboard.
    """
    today = datetime.now().strftime("%d %b %Y")

    # ── Header Banner ────────────────────────────────────────────────────────
    banner_text = Text()
    banner_text.append("🚀 INDIAN STOCK MARKET MULTIBAGGER BOT 🚀\n", style="bold white on blue")
    banner_text.append(f"   Target: 2x Returns in 1 Year (+100%) | Date: {today}\n", style="bold yellow")
    banner_text.append(f"   Universe: {config.universe.upper()} | Strategy: Stan Weinstein Stage-2 + Minervini + ML Ensemble\n", style="dim white")
    banner_text.append(f"   Risk Cushion: 200-DMA Trailing Stop Floor | Capital Horizon: 252 Trading Days", style="cyan")

    console.print(Panel(banner_text, border_style="bright_blue", box=box.ROUNDED))

    # ── Macro Environment Dashboard (USD/INR, Fed Rate, US 10Y Yield, Crude Oil)
    if macro_env is not None:
        macro_text = Text()
        macro_text.append("🌐 GLOBAL MACRO & INDIAN LIQUIDITY REGIME\n", style="bold magenta")
        
        # Currency line
        curr_style = "bold green" if macro_env.usd_inr_30d_change_pct < 1.0 else "bold yellow"
        macro_text.append("• 💵 USD / INR: ", style="bold white")
        macro_text.append(f"₹{macro_env.usd_inr:.2f} ({macro_env.usd_inr_status}, {macro_env.usd_inr_30d_change_pct:+.1f}% 30d)  |  ", style=curr_style)
        
        # Fed Rate & Policy
        macro_text.append("🏛️ Fed Rate Proxy: ", style="bold white")
        macro_text.append(f"{macro_env.fed_rate_proxy:.2f}% ({macro_env.fed_stance})\n", style="cyan")

        # 10Y Yield & Crude
        yield_style = "bold green" if macro_env.us_10y_yield < 4.2 else ("bold yellow" if macro_env.us_10y_yield < 4.6 else "bold red")
        macro_text.append("• 📈 US 10Y Bond Yield: ", style="bold white")
        macro_text.append(f"{macro_env.us_10y_yield:.2f}% ({macro_env.yield_regime})  |  ", style=yield_style)

        crude_style = "bold green" if macro_env.crude_oil_price < 76.0 else ("bold yellow" if macro_env.crude_oil_price < 85.0 else "bold red")
        macro_text.append("🛢️ Brent Crude: ", style="bold white")
        macro_text.append(f"${macro_env.crude_oil_price:.2f}/bbl ({macro_env.crude_impact_label})\n", style=crude_style)

        # Macro Score & Guidance
        score_style = "bold bright_green" if macro_env.macro_score >= 70 else ("bold yellow" if macro_env.macro_score >= 50 else "bold red")
        macro_text.append(f"• 🌡️ Macro Liquidity Score: ", style="bold white")
        macro_text.append(f"{macro_env.macro_score:.1f}/100 [{macro_env.regime_label}]  |  ", style=score_style)
        macro_text.append(f"Alloc Advice: {macro_env.capital_allocation_pct:.0f}% Capital\n", style="bold yellow")
        macro_text.append(f"  Guidance: {macro_env.allocation_guidance}", style="italic white")

        border = "bright_green" if macro_env.macro_score >= 70 else ("yellow" if macro_env.macro_score >= 50 else "red")
        console.print(Panel(macro_text, title="🌍 Macroeconomic Headwinds & Tailwinds", border_style=border, box=box.ROUNDED))

    if not candidates:
        console.print("[bold red]No stocks passed the strict Multibagger screening criteria today.[/]")
        return


    # ── Main Multibagger Radar Table ─────────────────────────────────────────
    table = Table(
        title=f"🎯 Top {len(candidates)} High-Conviction Multibagger Candidates (2x in 1 Year)",
        box=box.HEAVY_EDGE,
        header_style="bold cyan",
        title_style="bold magenta",
        show_lines=True
    )

    table.add_column("#", style="dim", justify="center", width=3)
    table.add_column("Ticker", style="bold white", width=11)
    table.add_column("CMP (₹)", justify="right", width=9)
    table.add_column("1Y 2x Goal", justify="right", style="bold bright_green", width=10)
    table.add_column("DCF 1Y FV", justify="right", style="cyan", width=10)
    table.add_column("GBM 1Y", justify="right", style="blue", width=10)
    table.add_column("Monte Carlo", justify="right", style="yellow", width=11)
    table.add_column("ARIMA 1Y", justify="right", style="magenta", width=10)
    table.add_column("Deep Learn", justify="right", style="bright_cyan", width=11)
    table.add_column("Consensus", justify="right", style="bold bright_green", width=11)
    table.add_column("2x Prob", justify="center", style="bold yellow", width=8)
    table.add_column("Score", justify="center", width=6)
    table.add_column("Grade", justify="center", width=6)

    for i, c in enumerate(candidates, 1):
        grade_style = _grade_color(c.grade)
        score_style = _score_color(c.multibagger_score)
        
        dcf_str = f"₹{c.dcf_future_value_1yr:,.0f}" if c.dcf_future_value_1yr > 0 else "-"
        gbm_str = f"₹{c.gbm_expected_price:,.0f}" if c.gbm_expected_price > 0 else "-"
        mc_str = f"₹{c.mc_median_price:,.0f}" if c.mc_median_price > 0 else "-"
        arima_str = f"₹{c.arima_forecast_price:,.0f}" if c.arima_forecast_price > 0 else "-"
        dl_str = f"₹{c.dl_lstm_target_price:,.0f}" if c.dl_lstm_target_price > 0 else "-"
        cons_str = f"₹{c.quant_consensus_target:,.0f}" if c.quant_consensus_target > 0 else "-"

        table.add_row(
            str(i),
            f"{c.ticker.replace('.NS', '')}\n[dim]{c.company_name[:10]}[/]",
            _format_price(c.cmp),
            f"{_format_price(c.target_1yr_2x)}\n[dim](+100%)[/]",
            dcf_str,
            gbm_str,
            mc_str,
            arima_str,
            dl_str,
            f"[bold bright_green]{cons_str}[/]",
            f"[bold]{c.ml_2x_prob_pct:.0f}%[/]",
            f"[{score_style}]{c.multibagger_score:.1f}[/]",
            f"[{grade_style}]{c.grade}[/]"
        )

    console.print(table)

    # ── Top 3 Deep-Dive Diagnosis Cards ──────────────────────────────────────
    top_3 = candidates[:3]
    for idx, c in enumerate(top_3, 1):
        card = Text()
        card.append(f"Rank #{idx} -- {c.company_name} ({c.ticker.replace('.NS', '')})  |  Grade: {c.grade}  |  Composite Score: {c.multibagger_score}/100\n", style="bold white")
        card.append(f"• Current Price: {_format_price(c.cmp)}  -->  1-Year 2x Target: {_format_price(c.target_1yr_2x)} (+100%)  -->  200-DMA Stop: {_format_price(c.stop_loss)}\n", style="bright_yellow")
        
        # Individual Model Breakdown
        card.append("• 🔬 Individual Quantitative Model Forecasts:\n", style="bold magenta")
        card.append(f"  1) 💵 DCF (Discounted Cash Flow): Fair Value ₹{c.dcf_intrinsic_value:,.1f} | 1Y Future Value: ₹{c.dcf_future_value_1yr:,.1f} (Score: {c.dcf_score:.0f}/100)\n", style="cyan")
        card.append(f"  2) 📈 Geometric Brownian Motion (GBM): Expected 1Y Price: ₹{c.gbm_expected_price:,.1f} ({c.gbm_1yr_return_pct:+.1f}%)\n", style="blue")
        card.append(f"  3) 🎲 Monte Carlo Simulation (5,000 Paths): Median: ₹{c.mc_median_price:,.1f} | Bull (75th): ₹{c.mc_bull_price:,.1f} | 2x Hit Rate: {c.mc_2x_prob_pct:.1f}%\n", style="yellow")
        card.append(f"  4) 📉 ARIMA / ARMA Time-Series: 1Y Forecast: ₹{c.arima_forecast_price:,.1f} ({c.arima_return_pct:+.1f}%)\n", style="magenta")
        card.append(f"  5) 🧠 Deep Learning (LSTM & Neural Net): 1Y Target: ₹{c.dl_lstm_target_price:,.1f} ({c.dl_lstm_return_pct:+.1f}%)\n", style="bright_cyan")
        card.append(f"  🎯 Multi-Model Consensus Target: ₹{c.quant_consensus_target:,.1f} ({c.quant_consensus_return_pct:+.1f}% Upside)\n", style="bold bright_green")

        card.append(f"• Fundamental Moat: ROCE {c.roce_pct}% | ROE {c.roe_pct}% | PAT Growth +{c.profit_growth_pct}% | Debt/Equity {c.debt_to_equity} | PEG {c.peg_ratio}\n", style="green")
        card.append(f"• Technical Breakout: Distance to 52W High: -{c.pct_from_52w_high:.1f}% | Minervini Score: {c.minervini_score}/8 | Up/Down Vol: {c.up_down_volume_ratio}x\n", style="cyan")
        card.append(f"• Machine Learning Ensemble: {c.ml_2x_prob_pct}% 2x Win Probability\n", style="bold yellow")
        if c.top_drivers:
            card.append(f"  Key Drivers: {', '.join(c.top_drivers)}\n", style="dim")
        if c.primary_catalyst:
            card.append(f"• Latest Catalyst: \"{c.primary_catalyst}\"\n", style="italic white")

        console.print(Panel(card, title=f"💎 High-Conviction Multibagger Diagnosis #{idx}", border_style="green", box=box.ROUNDED))



    # ── Wealth Simulator Projection ──────────────────────────────────────────
    sim_table = Table(
        title="💰 1–2 Year Wealth Compounding Simulator (₹5,00,000 Portfolio)",
        box=box.ROUNDED,
        header_style="bold yellow",
        show_lines=False
    )
    sim_table.add_column("Horizon", style="bold white")
    sim_table.add_column("Expected Portfolio Return", justify="center")
    sim_table.add_column("Projected Portfolio Value", justify="right", style="bold bright_green")
    sim_table.add_column("Absolute Gain (₹)", justify="right", style="green")

    sim_table.add_row("Starting Capital", "0.0%", "₹5,00,000", "₹0")
    sim_table.add_row("6-Month Target Milestone", "+40.0%", "₹7,00,000", "+₹2,00,000")
    sim_table.add_row("1-Year Multibagger 2x Goal", "+100.0%", "₹10,00,000", "+₹5,00,000 (DOUBLED)")
    sim_table.add_row("2-Year Compounding (Stretch)", "+175.0%", "₹13,75,000", "+₹8,75,000 (2.75x)")

    console.print(sim_table)


def export_csv(candidates: List[MultibaggerCandidate], filename: str = ""):
    """Export candidates to CSV file."""
    if not filename:
        date_str = datetime.now().strftime("%Y%m%d_%H%M")
        filename = f"multibagger_picks_{date_str}.csv"

    fieldnames = [
        "rank", "ticker", "company_name", "sector", "cmp", "target_1yr_2x",
        "target_6mo", "stop_loss", "risk_reward", "multibagger_score", "grade",
        "dcf_intrinsic_value", "dcf_future_value_1yr", "dcf_future_value_5yr",
        "dcf_margin_of_safety_pct", "dcf_score", "dcf_verdict",
        "gbm_expected_price", "gbm_1yr_return_pct",
        "mc_median_price", "mc_bull_price", "mc_2x_prob_pct",
        "arima_forecast_price", "arima_return_pct",
        "dl_lstm_target_price", "dl_lstm_return_pct",
        "quant_consensus_target", "quant_consensus_return_pct",
        "ml_2x_prob_pct", "moat_score", "sales_growth_pct", "profit_growth_pct",
        "roce_pct", "roe_pct", "debt_to_equity", "peg_ratio", "minervini_score",
        "is_stage2", "pct_from_52w_high", "delivery_pct", "primary_catalyst"
    ]



    try:
        with open(filename, "w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            for i, c in enumerate(candidates, 1):
                writer.writerow({
                    "rank": i,
                    "ticker": c.ticker,
                    "company_name": c.company_name,
                    "sector": c.sector,
                    "cmp": c.cmp,
                    "target_1yr_2x": c.target_1yr_2x,
                    "target_6mo": c.target_6mo,
                    "stop_loss": c.stop_loss,
                    "risk_reward": c.risk_reward,
                    "multibagger_score": c.multibagger_score,
                    "grade": c.grade,
                    "dcf_intrinsic_value": c.dcf_intrinsic_value,
                    "dcf_future_value_1yr": c.dcf_future_value_1yr,
                    "dcf_future_value_5yr": c.dcf_future_value_5yr,
                    "dcf_margin_of_safety_pct": c.dcf_margin_of_safety_pct,
                    "dcf_score": c.dcf_score,
                    "dcf_verdict": c.dcf_verdict,
                    "gbm_expected_price": c.gbm_expected_price,
                    "gbm_1yr_return_pct": c.gbm_1yr_return_pct,
                    "mc_median_price": c.mc_median_price,
                    "mc_bull_price": c.mc_bull_price,
                    "mc_2x_prob_pct": c.mc_2x_prob_pct,
                    "arima_forecast_price": c.arima_forecast_price,
                    "arima_return_pct": c.arima_return_pct,
                    "dl_lstm_target_price": c.dl_lstm_target_price,
                    "dl_lstm_return_pct": c.dl_lstm_return_pct,
                    "quant_consensus_target": c.quant_consensus_target,
                    "quant_consensus_return_pct": c.quant_consensus_return_pct,
                    "ml_2x_prob_pct": c.ml_2x_prob_pct,


                    "moat_score": c.moat_score,
                    "sales_growth_pct": c.sales_growth_pct,
                    "profit_growth_pct": c.profit_growth_pct,
                    "roce_pct": c.roce_pct,
                    "roe_pct": c.roe_pct,
                    "debt_to_equity": c.debt_to_equity,
                    "peg_ratio": c.peg_ratio,
                    "minervini_score": c.minervini_score,
                    "is_stage2": c.is_stage2,
                    "pct_from_52w_high": c.pct_from_52w_high,
                    "delivery_pct": c.delivery_pct,
                    "primary_catalyst": c.primary_catalyst
                })
        console.print(f"[bold green]✔ Successfully exported top picks to CSV:[/] [bold cyan]{filename}[/]")
    except Exception as e:
        console.print(f"[bold red]Failed to export CSV:[/] {e}")
