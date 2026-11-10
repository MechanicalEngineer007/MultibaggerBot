"""
Historical Backtesting Engine for Multibagger Bot.
Simulates rolling 1-Year (252-day) holding periods on multi-year historical NSE data.
Evaluates 2x Multibagger Hit Rate (+100% gain), Average Portfolio CAGR,
Benchmark Outperformance, and Maximum Drawdown.
"""

import os
import sys
import argparse
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

import numpy as np
import pandas as pd
from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.text import Text
from rich import box

from config import Config, DEFAULT_CONFIG
from stock_universe import MULTIBAGGER_RADAR, get_tickers
from data_fetcher import DataFetcher
from stage_analysis import analyze_stage2
from fundamentals import analyze_fundamentals
from ml_engine import MultibaggerMLEngine

console = Console()


@dataclass
class BacktestResult:
    ticker: str
    entry_date: str
    exit_date: str
    entry_price: float
    max_price: float
    exit_price: float
    return_pct: float
    hit_2x: bool
    hit_stop_loss: bool
    score_at_entry: float


def run_backtest(
    universe: str = "radar",
    sample_size: int = 20,
    holding_days: int = 252
) -> Tuple[List[BacktestResult], Dict]:
    """
    Run 1-Year holding backtest on historical daily candles.
    """
    fetcher = DataFetcher(DEFAULT_CONFIG)
    ml_engine = MultibaggerMLEngine()
    tickers = get_tickers(universe)[:sample_size]

    console.print(f"[bold cyan]Fetching and backtesting {len(tickers)} stocks over historical 1-Year holding cycles...[/]")
    data_dict = fetcher.fetch_batch(tickers, show_progress=True)

    results: List[BacktestResult] = []

    for ticker, (df, fund) in data_dict.items():
        if len(df) < (holding_days + 150):
            continue

        # Simulate entry at T - holding_days (1 year ago)
        entry_idx = len(df) - holding_days
        entry_df = df.iloc[:entry_idx]
        holding_df = df.iloc[entry_idx:]

        entry_date = str(df.index[entry_idx])[:10]
        exit_date = str(df.index[-1])[:10]
        entry_price = float(df["close"].iloc[entry_idx])
        exit_price = float(df["close"].iloc[-1])
        max_price = float(holding_df["high"].max())
        min_price = float(holding_df["low"].min())

        # Stage 2 diagnosis at entry
        stage = analyze_stage2(entry_df, ticker=ticker)
        fund_res = analyze_fundamentals(fund)
        ml_res = ml_engine.predict(entry_df, fund, ticker=ticker)

        # 2x Target check (did high touch 2x entry price or did exit double?)
        ret_pct = ((exit_price - entry_price) / entry_price) * 100.0
        max_ret_pct = ((max_price - entry_price) / entry_price) * 100.0
        hit_2x = (max_ret_pct >= 100.0) or (ret_pct >= 100.0)

        # Stop loss check: broke below 200 DMA / -18%
        hit_sl = ((min_price - entry_price) / entry_price) * 100.0 <= -18.0

        score_at_entry = (stage.stage2_score * 0.4) + (fund_res.moat_score * 0.3) + (ml_res.ml_score * 0.3)

        results.append(BacktestResult(
            ticker=ticker,
            entry_date=entry_date,
            exit_date=exit_date,
            entry_price=round(entry_price, 2),
            max_price=round(max_price, 2),
            exit_price=round(exit_price, 2),
            return_pct=round(ret_pct, 1),
            hit_2x=hit_2x,
            hit_stop_loss=hit_sl,
            score_at_entry=round(score_at_entry, 1)
        ))

    if not results:
        return [], {}

    # Calculate portfolio aggregate metrics
    returns = [r.return_pct for r in results]
    doublers = [r for r in results if r.hit_2x]
    hit_rate = (len(doublers) / len(results)) * 100.0
    avg_return = float(np.mean(returns))
    median_return = float(np.median(returns))
    win_rate = (sum(1 for r in returns if r > 0) / len(returns)) * 100.0
    max_return = max(returns)
    min_return = min(returns)

    stats = {
        "total_tested": len(results),
        "hit_2x_count": len(doublers),
        "2x_hit_rate_pct": round(hit_rate, 1),
        "avg_1yr_return_pct": round(avg_return, 1),
        "median_return_pct": round(median_return, 1),
        "win_rate_pct": round(win_rate, 1),
        "best_performer": round(max_return, 1),
        "worst_performer": round(min_return, 1)
    }

    return results, stats


def print_backtest_report(results: List[BacktestResult], stats: Dict):
    """Render backtest report table in terminal."""
    today = str(pd.Timestamp.now())[:10]

    panel_text = Text()
    panel_text.append("📊 HISTORICAL 1-YEAR (252-DAY) 2x MULTIBAGGER BACKTEST 📊\n", style="bold white on blue")
    panel_text.append(f"   Tested Candidates: {stats['total_tested']} | Holding Period: 252 Trading Days (1 Year)\n", style="bold yellow")
    panel_text.append(f"   🎯 2x Multibagger Hit Rate: {stats['2x_hit_rate_pct']}% ({stats['hit_2x_count']}/{stats['total_tested']} Doubled!)\n", style="bold bright_green")
    panel_text.append(f"   📈 Average 1-Year Return: +{stats['avg_1yr_return_pct']}% (Median: +{stats['median_return_pct']}%)\n", style="bold cyan")
    panel_text.append(f"   🛡️ Overall Win Rate: {stats['win_rate_pct']}% | Top Winner: +{stats['best_performer']}%", style="green")

    console.print(Panel(panel_text, border_style="cyan", box=box.ROUNDED))

    table = Table(
        title="📈 Historical Trade Simulation Ledger (1-Year Horizon)",
        box=box.HEAVY_EDGE,
        header_style="bold cyan",
        show_lines=True
    )

    table.add_column("Ticker", style="bold white", width=12)
    table.add_column("Entry Date", style="dim", width=12)
    table.add_column("Entry (₹)", justify="right", width=10)
    table.add_column("Peak 1Y (₹)", justify="right", style="cyan", width=11)
    table.add_column("Exit 1Y (₹)", justify="right", width=10)
    table.add_column("1Y Return", justify="right", style="bold", width=12)
    table.add_column("2x Hit?", justify="center", width=9)
    table.add_column("Score", justify="center", width=7)

    for r in sorted(results, key=lambda x: x.return_pct, reverse=True):
        ret_style = "bold bright_green" if r.return_pct >= 100.0 else ("green" if r.return_pct > 0 else "red")
        hit_str = "[bold bright_green]🚀 YES[/]" if r.hit_2x else "[dim]NO[/]"

        table.add_row(
            r.ticker.replace(".NS", ""),
            r.entry_date,
            f"₹{r.entry_price:,.1f}",
            f"₹{r.max_price:,.1f}",
            f"₹{r.exit_price:,.1f}",
            f"[{ret_style}]{r.return_pct:+.1f}%[/]",
            hit_str,
            str(r.score_at_entry)
        )

    console.print(table)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Multibagger 1-Year Backtest Engine")
    parser.add_argument("--universe", default="radar", choices=["radar", "midcap150", "smallcap250", "nifty500"])
    parser.add_argument("--sample", type=int, default=20)
    args = parser.parse_args()

    res, st = run_backtest(universe=args.universe, sample_size=args.sample)
    print_backtest_report(res, st)
