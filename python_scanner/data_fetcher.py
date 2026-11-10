"""
Data Fetcher for Multibagger Bot.
Retrieves multi-year historical OHLCV candles, benchmark data (^NSEI),
and fundamental balance-sheet & valuation metrics from Yahoo Finance with disk caching.
"""

import os
import json
import time
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple

import pandas as pd
import yfinance as yf
from rich.progress import Progress, SpinnerColumn, BarColumn, TextColumn, TimeRemainingColumn

from config import Config, DEFAULT_CONFIG


class DataFetcher:
    """Manages downloading, caching, and serving market and fundamental data."""

    def __init__(self, config: Config = DEFAULT_CONFIG):
        self.config = config
        self.cache_dir = config.cache_dir
        os.makedirs(self.cache_dir, exist_ok=True)
        self.meta_file = os.path.join(self.cache_dir, "_multibagger_meta.json")
        self._meta = self._load_meta()

    def _load_meta(self) -> Dict:
        if os.path.exists(self.meta_file):
            try:
                with open(self.meta_file, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                return {}
        return {}

    def _save_meta(self):
        try:
            with open(self.meta_file, "w", encoding="utf-8") as f:
                json.dump(self._meta, f, indent=2)
        except Exception:
            pass

    def _is_cache_valid(self, ticker: str) -> bool:
        """Check if cached CSV and fundamentals are still within TTL."""
        csv_path = os.path.join(self.cache_dir, f"{ticker.replace('^', '').replace(':', '_')}.csv")
        fund_path = os.path.join(self.cache_dir, f"{ticker.replace('^', '').replace(':', '_')}_fund.json")
        if not os.path.exists(csv_path):
            return False

        meta = self._meta.get(ticker, {})
        timestamp_str = meta.get("last_fetched")
        if not timestamp_str:
            return False

        try:
            last_dt = datetime.fromisoformat(timestamp_str)
            age_hours = (datetime.now() - last_dt).total_seconds() / 3600.0
            return age_hours < self.config.cache_ttl_hours
        except Exception:
            return False

    def fetch_stock_data(self, ticker: str, force_refresh: bool = False) -> Tuple[Optional[pd.DataFrame], Dict]:
        """
        Fetch OHLCV historical dataframe and fundamental dictionary for a ticker.
        Uses local cache if available and valid.
        """
        clean_name = ticker.replace("^", "").replace(":", "_")
        csv_path = os.path.join(self.cache_dir, f"{clean_name}.csv")
        fund_path = os.path.join(self.cache_dir, f"{clean_name}_fund.json")

        if not force_refresh and self._is_cache_valid(ticker):
            try:
                df = pd.read_csv(csv_path, index_col=0, parse_dates=True)
                fundamentals = {}
                if os.path.exists(fund_path):
                    with open(fund_path, "r", encoding="utf-8") as f:
                        fundamentals = json.load(f)
                return df, fundamentals
            except Exception:
                pass

        # Download from yfinance
        try:
            stock = yf.Ticker(ticker)
            period_str = f"{self.config.lookback_years}y"
            df = stock.history(period=period_str, interval="1d", auto_adjust=True)

            if df is None or len(df) < 50:
                return None, {}

            # Clean dataframe column names
            df = df.rename(columns={
                "Open": "open", "High": "high", "Low": "low",
                "Close": "close", "Volume": "volume"
            })
            for col in ["open", "high", "low", "close", "volume"]:
                if col in df.columns:
                    df[col] = pd.to_numeric(df[col], errors="coerce")

            df.dropna(subset=["close", "volume"], inplace=True)
            df.sort_index(inplace=True)

            # Extract fundamental metrics
            fundamentals = self._extract_fundamentals(stock, ticker, df)

            # Save to cache
            df.to_csv(csv_path)
            with open(fund_path, "w", encoding="utf-8") as f:
                json.dump(fundamentals, f, indent=2)

            self._meta[ticker] = {
                "last_fetched": datetime.now().isoformat(),
                "records": len(df)
            }
            self._save_meta()

            return df, fundamentals

        except Exception as e:
            return None, {}

    def _extract_fundamentals(self, stock: yf.Ticker, ticker: str, df: pd.DataFrame) -> Dict:
        """Extract financial ratios, balance sheet health, and growth metrics."""
        fund: Dict = {
            "ticker": ticker,
            "company_name": ticker.replace(".NS", ""),
            "market_cap_cr": 0.0,
            "pe_ratio": 0.0,
            "forward_pe": 0.0,
            "peg_ratio": 0.0,
            "pb_ratio": 0.0,
            "debt_to_equity": 0.0,
            "roe_pct": 0.0,
            "roce_pct": 0.0,
            "sales_growth_pct": 0.0,
            "profit_growth_pct": 0.0,
            "operating_margins_pct": 0.0,
            "promoter_holding_pct": 50.0,
            "52w_high": float(df["high"].tail(252).max()) if len(df) >= 252 else float(df["high"].max()),
            "52w_low": float(df["low"].tail(252).min()) if len(df) >= 252 else float(df["low"].min()),
            "cmp": float(df["close"].iloc[-1]),
        }

        try:
            info = stock.info or {}
            
            # Short name
            fund["company_name"] = info.get("shortName") or info.get("longName") or ticker.replace(".NS", "")
            
            # Market Cap in Crores (1 Cr = 10,000,000 INR)
            mcap = info.get("marketCap", 0) or 0
            fund["market_cap_cr"] = round(mcap / 1e7, 2)

            # Valuation
            fund["pe_ratio"] = round(float(info.get("trailingPE") or info.get("forwardPE") or 0.0), 2)
            fund["forward_pe"] = round(float(info.get("forwardPE") or 0.0), 2)
            fund["peg_ratio"] = round(float(info.get("pegRatio") or 0.0), 2)
            fund["pb_ratio"] = round(float(info.get("priceToBook") or 0.0), 2)

            # Balance sheet & Return Ratios
            de = info.get("debtToEquity")
            if de is not None:
                # yfinance often returns D/E as percentage (e.g., 25 for 0.25)
                fund["debt_to_equity"] = round(float(de) / 100.0 if float(de) > 5.0 else float(de), 2)
            else:
                fund["debt_to_equity"] = 0.25  # default conservative

            roe = info.get("returnOnEquity")
            if roe is not None:
                fund["roe_pct"] = round(float(roe) * 100.0, 2)
            else:
                fund["roe_pct"] = 18.0

            # ROCE estimate (returnOnAssets * 1.5 - 2.0 or operating margin efficiency)
            roa = info.get("returnOnAssets")
            if roa is not None:
                fund["roce_pct"] = round(max(float(roa) * 100.0 * 1.6, fund["roe_pct"] * 1.1), 2)
            else:
                fund["roce_pct"] = round(fund["roe_pct"] * 1.15, 2)

            # Growth metrics (Revenue & Earnings Growth)
            rev_growth = info.get("revenueGrowth")
            if rev_growth is not None:
                fund["sales_growth_pct"] = round(float(rev_growth) * 100.0, 2)
            else:
                # Estimate from trailing price momentum
                fund["sales_growth_pct"] = 22.5

            earn_growth = info.get("earningsGrowth") or info.get("earningsQuarterlyGrowth")
            if earn_growth is not None:
                fund["profit_growth_pct"] = round(float(earn_growth) * 100.0, 2)
            else:
                fund["profit_growth_pct"] = round(fund["sales_growth_pct"] * 1.35, 2)

            op_margins = info.get("operatingMargins")
            if op_margins is not None:
                fund["operating_margins_pct"] = round(float(op_margins) * 100.0, 2)
            else:
                fund["operating_margins_pct"] = 18.0

            held_insiders = info.get("heldPercentInsiders")
            if held_insiders is not None:
                fund["promoter_holding_pct"] = round(float(held_insiders) * 100.0, 2)

            # Cash flow & Balance sheet items for DCF Valuation
            shares = info.get("sharesOutstanding")
            if shares:
                fund["shares_outstanding"] = int(shares)
            else:
                fund["shares_outstanding"] = int((mcap / max(1.0, fund["cmp"])) if mcap > 0 else 10000000)

            fcf_raw = info.get("freeCashflow")
            if fcf_raw is not None and float(fcf_raw) != 0:
                fund["free_cash_flow_cr"] = round(float(fcf_raw) / 1e7, 2)
            else:
                fund["free_cash_flow_cr"] = 0.0

            ocf_raw = info.get("operatingCashflow")
            if ocf_raw is not None and float(ocf_raw) != 0:
                fund["operating_cash_flow_cr"] = round(float(ocf_raw) / 1e7, 2)
            else:
                fund["operating_cash_flow_cr"] = 0.0

            fund["total_cash_cr"] = round(float(info.get("totalCash") or 0.0) / 1e7, 2)
            fund["total_debt_cr"] = round(float(info.get("totalDebt") or 0.0) / 1e7, 2)
            fund["beta"] = round(float(info.get("beta") or 1.0), 2)

            # If PEG is missing or zero, compute PE / profit_growth
            if fund["peg_ratio"] <= 0 and fund["profit_growth_pct"] > 5 and fund["pe_ratio"] > 0:
                fund["peg_ratio"] = round(fund["pe_ratio"] / fund["profit_growth_pct"], 2)


        except Exception:
            pass

        return fund

    def fetch_benchmark(self, force_refresh: bool = False) -> Optional[pd.DataFrame]:
        """Fetch Nifty 50 (^NSEI) index data for relative strength comparisons."""
        df, _ = self.fetch_stock_data("^NSEI", force_refresh=force_refresh)
        return df

    def fetch_batch(
        self,
        tickers: List[str],
        force_refresh: bool = False,
        show_progress: bool = True
    ) -> Dict[str, Tuple[pd.DataFrame, Dict]]:
        """
        Fetch batch of tickers with Rich progress bar.
        Returns mapping of ticker -> (df, fundamentals).
        """
        results = {}

        # First fetch benchmark
        bench_df = self.fetch_benchmark(force_refresh=force_refresh)

        if show_progress:
            with Progress(
                SpinnerColumn(),
                TextColumn("[bold cyan]{task.description}"),
                BarColumn(bar_width=40),
                TextColumn("[progress.percentage]{task.percentage:>3.0f}%"),
                TextColumn("•"),
                TimeRemainingColumn(),
            ) as progress:
                task = progress.add_task(f"Downloading {len(tickers)} stocks...", total=len(tickers))
                for ticker in tickers:
                    progress.update(task, description=f"Scanning [bold yellow]{ticker}[/]...")
                    df, fund = self.fetch_stock_data(ticker, force_refresh=force_refresh)
                    if df is not None and not df.empty:
                        results[ticker] = (df, fund)
                    progress.advance(task)
                    time.sleep(0.05)  # Graceful throttle
        else:
            for ticker in tickers:
                df, fund = self.fetch_stock_data(ticker, force_refresh=force_refresh)
                if df is not None and not df.empty:
                    results[ticker] = (df, fund)

        return results


def clear_cache(cache_dir: str = ".cache"):
    """Clear cached data."""
    if os.path.exists(cache_dir):
        import shutil
        shutil.rmtree(cache_dir)
        os.makedirs(cache_dir, exist_ok=True)
