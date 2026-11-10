"""
Indian Stock Market Multibagger Bot
===================================

Scans NSE Midcap & Smallcap emerging leaders using Stan Weinstein Stage-2 Breakouts,
Mark Minervini's Trend Template, Fundamental Moat (Sales/PAT CAGR, ROCE, Low Debt),
and Machine Learning Ensemble to identify 2x Multibagger stocks in 1 year.

Usage:
    python main.py                          # Full scan of Multibagger Radar
    python main.py --universe midcap150     # Scan Nifty Midcap 150
    python main.py --universe smallcap250   # Scan Nifty Smallcap 250
    python main.py --tickers BEL,HAL,TRENT  # Custom stock diagnosis
    python main.py --top 10 --export        # Top 10 picks + CSV export
    python main.py --refresh                # Force re-download data
    python main.py --train-ml               # Train ML 2x model on historical data
    python main.py --backtest               # Run 1-Year historical backtest
    python main.py --telegram               # Broadcast top picks to Telegram

Author: Multibagger Bot
Disclaimer: For research & educational purposes only. Not financial advice.
"""

import os
import sys
import warnings
warnings.filterwarnings("ignore")

# Fix Windows console encoding for Unicode and Emoji
os.environ["PYTHONIOENCODING"] = "utf-8"
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

import argparse
import time
from datetime import datetime

from rich.console import Console
from rich.panel import Panel
from rich.text import Text

from config import (
    Config, DEFAULT_CONFIG,
    UNIVERSE_RADAR, UNIVERSE_MIDCAP150, UNIVERSE_SMALLCAP250, UNIVERSE_NIFTY500, UNIVERSE_CUSTOM
)
from stock_universe import get_tickers
from data_fetcher import DataFetcher, clear_cache
from stage_analysis import analyze_stage2
from fundamentals import analyze_fundamentals
from dcf_model import calculate_dcf
from quantitative_models import QuantitativeForecaster
from ml_engine import MultibaggerMLEngine
from delivery_tracker import DeliveryTracker
from macro_regime import MacroRegimeEngine
from news_sentiment import analyze_batch as analyze_news_batch
from scorer import score_candidate, rank_candidates
from report import print_multibagger_report, export_csv
from telegram_alerts import broadcast_candidates

console = Console()


def parse_args():
    """Parse command-line arguments."""
    parser = argparse.ArgumentParser(
        description="🚀 Indian Stock Market Multibagger Bot (Target: 2x in 1 Year)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python main.py                          Full scan with defaults (Multibagger Radar)
  python main.py --universe midcap150     Scan Nifty Midcap 150
  python main.py --tickers BEL,HAL,DIXON  Scan specific tickers
  python main.py --top 10 --export        Top 10 picks + CSV export
  python main.py --train-ml               Train ML model on historical 2x cycles
  python main.py --backtest               Run 1-Year historical simulation
  python main.py --telegram               Send high-conviction picks to Telegram

Disclaimer: For research & educational purposes only. Not financial advice.
        """
    )

    parser.add_argument(
        "--universe", "-u",
        choices=[UNIVERSE_RADAR, UNIVERSE_MIDCAP150, UNIVERSE_SMALLCAP250, UNIVERSE_NIFTY500, UNIVERSE_CUSTOM],
        default=UNIVERSE_RADAR,
        help="Stock universe to scan (default: radar)"
    )
    parser.add_argument(
        "--tickers",
        type=str,
        default="",
        help="Comma-separated custom ticker list (e.g. BEL,HAL,TRENT,DIXON)"
    )
    parser.add_argument(
        "--top", "-t",
        type=int,
        default=15,
        help="Number of top multibagger picks to show (default: 15)"
    )
    parser.add_argument(
        "--export", "-e",
        action="store_true",
        help="Export results to CSV file"
    )
    parser.add_argument(
        "--refresh", "-r",
        action="store_true",
        help="Force re-download all market and fundamental data"
    )
    parser.add_argument(
        "--no-news",
        action="store_true",
        help="Skip news and catalyst sentiment analysis (faster execution)"
    )
    parser.add_argument(
        "--train-ml",
        action="store_true",
        help="Train the Machine Learning 2x predictor model on historical data"
    )
    parser.add_argument(
        "--backtest",
        action="store_true",
        help="Run 1-Year historical backtest simulation"
    )
    parser.add_argument(
        "--telegram",
        action="store_true",
        help="Broadcast top alerts to Telegram"
    )
    parser.add_argument(
        "--audit",
        action="store_true",
        help="Audit active recommendations for fundamental deterioration and dump warnings"
    )

    return parser.parse_args()


def main():
    args = parse_args()

    # Route: Audit active recommendations
    if args.audit:
        from market_scheduler import run_market_audit
        run_market_audit(session_name="Manual Portfolio Health & Dump Audit", send_telegram=args.telegram)
        return

    # Route: Backtest mode
    if args.backtest:
        from backtest import run_backtest, print_backtest_report
        results, stats = run_backtest(universe=args.universe, sample_size=args.top)
        print_backtest_report(results, stats)
        return

    # Configuration setup
    config = Config(
        universe=args.universe,
        top_n=args.top,
        export_csv=args.export
    )

    if args.refresh:
        console.print("[yellow]Forcing re-download of market data (clearing cache)...[/]")
        clear_cache(config.cache_dir)

    # Determine tickers to scan
    if args.tickers:
        raw_list = [t.strip().upper() for t in args.tickers.split(",") if t.strip()]
        tickers = get_tickers(UNIVERSE_CUSTOM, custom_list=raw_list)
        config.universe = UNIVERSE_CUSTOM
    else:
        tickers = get_tickers(config.universe)

    console.print(f"[bold cyan]🔍 Initializing Multibagger Scan across {len(tickers)} stocks in '{config.universe.upper()}' universe...[/]")
    start_time = time.time()

    fetcher = DataFetcher(config)
    ml_engine = MultibaggerMLEngine()
    delivery_tracker = DeliveryTracker()
    macro_engine = MacroRegimeEngine(cache_dir=config.cache_dir)
    quant_forecaster = QuantitativeForecaster(simulation_paths=3000, forecast_days=252)

    # 1. Fetch Global Macro Liquidity Regime (USD/INR, Fed Rate, US 10Y Yield, Crude Oil)
    console.print("[bold cyan]🌐 Assessing Global Macro Regime (USD/INR, Fed Rate, US 10Y Yield, Crude Oil)...[/]")
    macro_env = macro_engine.get_macro_environment(force_refresh=args.refresh)

    # 2. Fetch Market Data & Benchmark
    data_dict = fetcher.fetch_batch(tickers, force_refresh=args.refresh, show_progress=True)
    bench_df = fetcher.fetch_benchmark()

    if not data_dict:
        console.print("[bold red]❌ Failed to retrieve stock data. Check your network connection.[/]")
        return

    # Route: Train ML model if requested
    if args.train_ml:
        console.print("[bold yellow]🤖 Training Machine Learning 2x Multibagger Ensemble on historical cycles...[/]")
        train_res = ml_engine.train(data_dict)
        console.print(f"[bold green]✔ Model training finished:[/] {train_res}")
        return

    # 3. Stage-2 Technical & Fundamental Moat Diagnosis
    console.print("[bold cyan]📊 Evaluating Minervini Trend Templates, DCF & Quantitative Forecasting Suite...[/]")
    raw_candidates = []

    for ticker, (df, fund) in data_dict.items():
        try:
            stage2_res = analyze_stage2(df, benchmark_df=bench_df, config=config, ticker=ticker)
            fund_res = analyze_fundamentals(fund, config=config)
            dcf_res = calculate_dcf(fund, macro_env=macro_env, config=config)
            quant_suite = quant_forecaster.forecast_all(df, ticker=ticker)
            ml_res = ml_engine.predict(df, fund, ticker=ticker, macro_env=macro_env)
            deliv_res = delivery_tracker.analyze(df, ticker=ticker)

            raw_candidates.append({
                "ticker": ticker,
                "stage2": stage2_res,
                "fund": fund_res,
                "dcf": dcf_res,
                "quant": quant_suite,
                "ml": ml_res,
                "deliv": deliv_res
            })
        except Exception as e:
            continue

    # 4. News Sentiment Analysis (if enabled)
    news_dict = {}
    if not args.no_news:
        top_prelim = sorted(raw_candidates, key=lambda x: x["stage2"].stage2_score + x["fund"].moat_score, reverse=True)[:args.top + 5]
        top_tickers = [c["ticker"] for c in top_prelim]
        console.print(f"[bold cyan]📰 Scanning financial catalysts & capex news for top candidates...[/]")
        news_dict = analyze_news_batch(top_tickers, config=config, delay=0.3)

    # 5. Composite Scoring & Ranking (with DCF & Macro Regime Adjustment)
    candidates = []
    for item in raw_candidates:
        t = item["ticker"]
        news_res = news_dict.get(t)
        c = score_candidate(
            ticker=t,
            fund=item["fund"],
            stage2=item["stage2"],
            ml_res=item["ml"],
            deliv=item["deliv"],
            news=news_res,
            macro_env=macro_env,
            dcf_res=item["dcf"],
            quant_suite=item["quant"],
            config=config
        )
        candidates.append(c)



    ranked_picks = rank_candidates(candidates, top_n=args.top)

    # 6. Output Report
    elapsed = round(time.time() - start_time, 1)
    console.print(f"\n[bold green]✔ Analysis completed in {elapsed}s across {len(candidates)} stocks.[/]")
    print_multibagger_report(ranked_picks, config=config, macro_env=macro_env)

    # 6.5 Record recommendations into persistent ledger for 09:15 AM & 03:30 PM tracking
    try:
        from recommendation_tracker import RecommendationTracker
        tracker = RecommendationTracker()
        added = tracker.record_recommendations(ranked_picks)
        if added > 0:
            console.print(f"[bold green]✔ Stored {added} new recommendations in persistent tracking ledger.[/]")
    except Exception as e:
        console.print(f"[yellow]Warning: Could not update recommendation ledger: {e}[/]")

    # 7. CSV Export
    if args.export:
        export_csv(ranked_picks)

    # 8. Telegram Alert Broadcast
    if args.telegram:
        sent = broadcast_candidates(ranked_picks, top_n=5, macro_env=macro_env)
        if sent > 0:
            console.print(f"[bold green]✔ Broadcasted {sent} Multibagger Alerts to Telegram![/]")



if __name__ == "__main__":
    main()
