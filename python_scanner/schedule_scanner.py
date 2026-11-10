"""
Scheduled Market Scanner & Alert Runner for Multibagger Bot
============================================================

Runs on a schedule or interval (e.g. daily at 3:00 PM IST or weekends),
scans the high-growth universe, runs ML 2x probability scoring,
and dispatches high-conviction alerts to Telegram.

Usage:
    python schedule_scanner.py --once
    python schedule_scanner.py --interval-hours 24
"""

import os
import sys
import argparse
import time
from datetime import datetime

from config import Config, DEFAULT_CONFIG
from stock_universe import get_tickers
from data_fetcher import DataFetcher
from stage_analysis import analyze_stage2
from fundamentals import analyze_fundamentals
from ml_engine import MultibaggerMLEngine
from delivery_tracker import DeliveryTracker
from scorer import score_candidate, rank_candidates
from telegram_alerts import broadcast_candidates, load_telegram_config


def run_scheduled_scan(universe: str = "radar", top_n: int = 5, min_score: float = 70.0):
    """Executes a single market scan pass and sends alerts for top setups."""
    now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"\n[{now_str}] 🚀 Starting Scheduled Multibagger 2x Scan on '{universe}'...")

    config = Config(universe=universe, top_n=top_n)
    fetcher = DataFetcher(config)
    ml_engine = MultibaggerMLEngine()
    delivery_tracker = DeliveryTracker()

    tickers = get_tickers(universe)
    data_dict = fetcher.fetch_batch(tickers, show_progress=False)
    bench_df = fetcher.fetch_benchmark()

    candidates = []
    for ticker, (df, fund) in data_dict.items():
        try:
            stage2 = analyze_stage2(df, benchmark_df=bench_df, config=config, ticker=ticker)
            fund_res = analyze_fundamentals(fund, config=config)
            ml_res = ml_engine.predict(df, fund, ticker=ticker)
            deliv_res = delivery_tracker.analyze(df, ticker=ticker)

            c = score_candidate(
                ticker=ticker,
                fund=fund_res,
                stage2=stage2,
                ml_res=ml_res,
                deliv=deliv_res,
                news=None,
                config=config
            )
            if c.passed_filters and c.multibagger_score >= min_score:
                candidates.append(c)
        except Exception:
            continue

    top_picks = rank_candidates(candidates, top_n=top_n)
    print(f"[{datetime.now().strftime('%H:%M:%S')}] Found {len(top_picks)} High-Conviction Multibagger Candidates!")

    if top_picks:
        sent = broadcast_candidates(top_picks, top_n=top_n)
        print(f"[{datetime.now().strftime('%H:%M:%S')}] Dispatched {sent} alerts to Telegram.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Multibagger Scheduled Scanner")
    parser.add_argument("--once", action="store_true", help="Run scan once and exit")
    parser.add_argument("--interval-hours", type=float, default=24.0, help="Interval between scans in hours")
    parser.add_argument("--universe", default="radar", help="Stock universe to scan")
    parser.add_argument("--min-score", type=float, default=70.0, help="Min score for alerts")
    args = parser.parse_args()

    if args.once:
        run_scheduled_scan(universe=args.universe, min_score=args.min_score)
    else:
        print(f"[*] Starting Multibagger Scheduler (Interval: {args.interval_hours} hours)... Press Ctrl+C to stop.")
        while True:
            try:
                run_scheduled_scan(universe=args.universe, min_score=args.min_score)
                time.sleep(args.interval_hours * 3600)
            except KeyboardInterrupt:
                print("\nScheduler terminated.")
                break
            except Exception as e:
                print(f"[!] Error in scheduled run: {e}")
                time.sleep(300)
