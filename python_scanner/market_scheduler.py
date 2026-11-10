"""
Multibagger Bot - Indian Market Timing Scheduler (09:15 AM & 03:30 PM IST)
===========================================================================
Monitors active multibagger recommendations twice daily:
  1. 🌅 Market Open (09:15 AM IST): Pre-market gaps & morning stop-loss audit.
  2. 🌇 Market Close (03:30 PM IST): End-of-day health audit, fundamental breakdown
     checks, quarterly filings check, and DUMP WARNING alert dispatch.

Usage:
  python market_scheduler.py --audit              Run audit immediately right now
  python market_scheduler.py --daemon             Run continuous scheduler (09:15 AM & 03:30 PM)
  python market_scheduler.py --telegram           Dispatch results & dump warnings to Telegram
"""

import os
import sys
import time
import argparse
from datetime import datetime

# Windows UTF-8 console output support
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

from rich.console import Console
from rich.panel import Panel

from config import Config
from data_fetcher import DataFetcher
from stock_universe import get_tickers
from stage_analysis import analyze_stage2
from fundamentals import analyze_fundamentals
from dcf_model import calculate_dcf
from quantitative_models import QuantitativeForecaster
from ml_engine import MultibaggerMLEngine
from delivery_tracker import DeliveryTracker
from macro_regime import MacroRegimeEngine
from scorer import score_candidate, rank_candidates
from recommendation_tracker import RecommendationTracker, print_audit_report
from telegram_alerts import load_telegram_config, send_telegram_message

console = Console()
tracker = RecommendationTracker()


def send_dump_telegram_alert(dump_records: list):
    """Sends immediate high-priority dump alerts to Telegram if configured."""
    config = load_telegram_config()
    if not config or not config.get("bot_token") or not config.get("chat_id"):
        return

    for r in dump_records:
        t = r["ticker"].replace(".NS", "")
        reasons = "\n• ".join(r.get("dump_warnings", ["Fundamental or Technical Breakdown"]))
        msg = (
            f"🚨 <b>DUMP WARNING: EXIT / TRIM RECOMMENDED</b> 🚨\n\n"
            f"<b>Stock:</b> {r.get('company_name', t)} ({t})\n"
            f"<b>Current Price:</b> ₹{r.get('current_price', 0.0):,.1f} | <b>P&L:</b> {r.get('pnl_pct', 0.0):+.1f}%\n"
            f"<b>200-DMA Stop:</b> ₹{r.get('stop_loss', 0.0):,.1f}\n\n"
            f"<b>Trigger Reasons:</b>\n• {reasons}\n\n"
            f"⚠️ <i>Recommendation: Fundamental degradation or stop breakdown detected. Capital protection advised.</i>"
        )
        send_telegram_message(config["bot_token"], config["chat_id"], msg)


def run_market_audit(session_name: str = "Live Audit", send_telegram: bool = False):
    """Runs a complete audit of all active stock recommendations."""
    now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    console.print(f"\n[bold cyan]━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━[/]")
    console.print(f"[bold yellow]⏱️ [{now_str}] Starting {session_name}...[/]")
    console.print(f"[bold cyan]━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━[/]")

    active_tickers = list(tracker.ledger.get("active_recommendations", {}).keys())

    if not active_tickers:
        console.print("[yellow]No active recommendations found in ledger. Running initial scan to populate...[/]")
        run_full_scan_and_record(universe="radar", top_n=10)
        active_tickers = list(tracker.ledger.get("active_recommendations", {}).keys())

    if not active_tickers:
        console.print("[red]No tickers to audit.[/]")
        return []

    config = Config(universe="custom")
    fetcher = DataFetcher(config)
    data_dict = fetcher.fetch_batch(active_tickers, force_refresh=True, show_progress=False)

    macro_engine = MacroRegimeEngine(cache_dir=config.cache_dir)
    macro_env = macro_engine.get_macro_environment(force_refresh=False)

    audit_results = tracker.audit_active_recommendations(data_dict, macro_env=macro_env)
    print_audit_report(audit_results, session_label=session_name)

    # Check for Dump Warnings
    dump_warnings = tracker.get_dump_warnings()
    if dump_warnings:
        console.print(f"\n[bold red]🚨 Detected {len(dump_warnings)} DUMP WARNING(S)![/]")
        for d in dump_warnings:
            t = d["ticker"].replace(".NS", "")
            console.print(f"  • [bold red]{t}[/]: " + " | ".join(d.get("dump_warnings", [])))
        if send_telegram:
            send_dump_telegram_alert(dump_warnings)
            console.print("[bold green]✔ Sent high-priority dump warning(s) to Telegram.[/]")

    return audit_results


def run_full_scan_and_record(universe: str = "radar", top_n: int = 15):
    """Scans the market universe, ranks top multibaggers, and saves them to persistent ledger."""
    console.print(f"[bold cyan]🔍 Scanning '{universe.upper()}' universe and updating ledger...[/]")
    config = Config(universe=universe, top_n=top_n)
    fetcher = DataFetcher(config)
    ml_engine = MultibaggerMLEngine()
    delivery_tracker = DeliveryTracker()
    macro_engine = MacroRegimeEngine(cache_dir=config.cache_dir)
    quant_forecaster = QuantitativeForecaster(simulation_paths=2000, forecast_days=252)

    macro_env = macro_engine.get_macro_environment()
    tickers = get_tickers(universe)
    data_dict = fetcher.fetch_batch(tickers, show_progress=False)
    bench_df = fetcher.fetch_benchmark()

    candidates = []
    for ticker, (df, fund) in data_dict.items():
        try:
            stage2_res = analyze_stage2(df, benchmark_df=bench_df, config=config, ticker=ticker)
            fund_res = analyze_fundamentals(fund, config=config)
            dcf_res = calculate_dcf(fund, macro_env=macro_env, config=config)
            quant_suite = quant_forecaster.forecast_all(df, ticker=ticker)
            ml_res = ml_engine.predict(df, fund, ticker=ticker, macro_env=macro_env)
            deliv_res = delivery_tracker.analyze(df, ticker=ticker)

            c = score_candidate(
                ticker=ticker,
                fund=fund_res,
                stage2=stage2_res,
                ml_res=ml_res,
                deliv=deliv_res,
                news=None,
                macro_env=macro_env,
                dcf_res=dcf_res,
                quant_suite=quant_suite,
                config=config
            )
            candidates.append(c)
        except Exception:
            continue

    top_picks = rank_candidates(candidates, top_n=top_n)
    added = tracker.record_recommendations(top_picks)
    console.print(f"[bold green]✔ Recorded {len(top_picks)} top recommendations into persistent ledger ({added} newly added).[/]")
    return top_picks


def run_scheduler_daemon(send_telegram: bool = True):
    """
    Continuous scheduler daemon that tracks Indian Market Timings:
      - 09:15 AM IST (Market Open Check)
      - 03:30 PM IST (Market Close Check & Rebalance)
    """
    console.print(Panel(
        "[bold cyan]🤖 Multibagger Bot Indian Market Timing Scheduler[/]\n"
        "• [bold green]09:15 AM IST:[/] Morning Market Open Check (Pre-Market Gap & Stop-Loss Audit)\n"
        "• [bold yellow]03:30 PM IST:[/] Market Close Audit & Fundamental Breakdown Check\n"
        "• [bold red]🚨 Dump Warnings:[/] Real-time alerts on PAT collapse or 200-DMA violations.\n\n"
        "[white]Press Ctrl+C to terminate scheduler.[/]",
        title="[bold green]Daemon Active[/]",
        border_style="cyan"
    ))

    last_morning_run_date = None
    last_evening_run_date = None

    while True:
        try:
            now = datetime.now()
            today_str = now.strftime("%Y-%m-%d")
            hour = now.hour
            minute = now.minute
            weekday = now.weekday()  # 0 = Mon, 4 = Fri, 5 = Sat, 6 = Sun

            # Monday through Friday check
            if weekday < 5:
                # 09:15 AM Check
                if hour == 9 and minute >= 15 and last_morning_run_date != today_str:
                    console.print(f"\n[bold green]🌅 09:15 AM Market Open Triggered![/]")
                    run_market_audit(session_name="🌅 09:15 AM Market Open Audit", send_telegram=send_telegram)
                    last_morning_run_date = today_str

                # 03:30 PM Check
                if hour == 15 and minute >= 30 and last_evening_run_date != today_str:
                    console.print(f"\n[bold yellow]🌇 03:30 PM Market Close Triggered![/]")
                    run_market_audit(session_name="🌇 03:30 PM Market Close Audit", send_telegram=send_telegram)
                    # Also scan for new emerging multibaggers for the next day
                    run_full_scan_and_record(universe="radar", top_n=10)
                    last_evening_run_date = today_str

            time.sleep(30)
        except KeyboardInterrupt:
            console.print("\n[yellow]Market Scheduler terminated by user.[/]")
            break
        except Exception as e:
            console.print(f"[red]Error in scheduler loop: {e}[/]")
            time.sleep(60)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Indian Market Timing Multibagger Scheduler")
    parser.add_argument("--audit", action="store_true", help="Run active recommendations audit right now")
    parser.add_argument("--scan", action="store_true", help="Run full scan and record recommendations")
    parser.add_argument("--daemon", action="store_true", help="Run standing 09:15 AM / 03:30 PM scheduler daemon")
    parser.add_argument("--telegram", action="store_true", help="Broadcast alerts to Telegram")
    args = parser.parse_args()

    if args.scan:
        run_full_scan_and_record(universe="radar", top_n=15)
    elif args.daemon:
        run_scheduler_daemon(send_telegram=args.telegram)
    else:
        # Default: audit active recommendations
        run_market_audit(session_name="Instant Health & Dump Audit", send_telegram=args.telegram)
