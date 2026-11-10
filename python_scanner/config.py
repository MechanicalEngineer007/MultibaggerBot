"""
Configuration and constants for the Indian Stock Market Multibagger Bot.
Targets 1-Year 2x Returns (100%+ Gain) using Machine Learning, Stage-2 Breakouts,
and Fundamental Moat Analysis.
"""

import os
from dataclasses import dataclass, field
from typing import Dict, List

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

# ─── Stock Universe Identifiers ───────────────────────────────────────────────

UNIVERSE_RADAR = "radar"             # Curated emerging leaders & high-beta compounders
UNIVERSE_MIDCAP150 = "midcap150"     # Nifty Midcap 150
UNIVERSE_SMALLCAP250 = "smallcap250" # Nifty Smallcap 250
UNIVERSE_NIFTY500 = "nifty500"       # Full Nifty 500
UNIVERSE_CUSTOM = "custom"           # Custom list of tickers

# ─── Configuration Dataclass ──────────────────────────────────────────────────

@dataclass
class Config:
    """Central configuration for Multibagger Bot."""

    # --- Stock Universe ---
    universe: str = UNIVERSE_RADAR
    custom_tickers: List[str] = field(default_factory=list)

    # --- Data Fetching ---
    lookback_years: int = 3             # 3 years daily data for ML & 200-DMA slope
    cache_dir: str = os.path.join(SCRIPT_DIR, ".cache")  # Local cache directory
    cache_ttl_hours: int = 12           # Cache time-to-live in hours

    # --- Multibagger Target & Horizon ---
    target_return_pct: float = 100.0    # 2x Return in 1 Year (+100%)
    holding_period_days: int = 252      # 1 Year (~252 NSE trading days)
    intermediate_target_pct: float = 40.0 # 6-Month intermediate milestone (+40%)
    trailing_sl_pct: float = 18.0       # Max initial risk cushion below entry
    use_200dma_cushion: bool = True     # Use 200-DMA as dynamic trailing structural floor

    # --- Fundamental Moat Screening ---
    min_sales_cagr_pct: float = 15.0    # Min 3Y Sales CAGR %
    min_pat_cagr_pct: float = 20.0      # Min 3Y Net Profit (PAT) CAGR %
    min_roce_pct: float = 15.0          # Min Return on Capital Employed %
    min_roe_pct: float = 14.0           # Min Return on Equity %
    max_debt_to_equity: float = 0.80    # Max D/E ratio (prefer low debt / net cash)
    max_peg_ratio: float = 2.20         # Max PEG (Growth at Reasonable Price)
    min_market_cap_cr: float = 1000.0   # Min ₹1,000 Cr market cap (liquidity safeguard)

    # --- Stage-2 Technical & Momentum Criteria ---
    minervini_strict: bool = True       # Enforce 50 EMA > 150 EMA > 200 EMA
    dma200_slope_lookback: int = 20     # 200-DMA slope must be positive over 20 days
    max_pct_from_52w_high: float = 25.0 # Must be within 25% of 52-Week High
    min_pct_above_52w_low: float = 30.0 # Must be at least 30% above 52-Week Low
    mansfield_rs_period: int = 50       # Relative strength vs NIFTY 50

    # --- Machine Learning Engine ---
    ml_model_file: str = "ml_multibagger_model.pkl"
    ml_target_threshold_pct: float = 100.0 # Label 1 if 1-yr forward return >= 100%
    ml_high_conviction_threshold: float = 0.60 # Min ML probability for High Conviction

    # --- News & Catalyst Sentiment ---
    news_lookback_days: int = 14        # Lookback for order wins, capex, earnings
    news_max_headlines: int = 15
    news_request_delay: float = 0.8

    # --- Composite Scoring Weights (Sum = 1.0) ---
    weight_fundamentals: float = 0.25   # Moat, Sales/PAT growth, ROCE, low debt
    weight_dcf: float = 0.20            # Discounted Cash Flow Intrinsic Value & Score
    weight_stage2: float = 0.20         # Minervini template, 200-DMA, 52W High proximity
    weight_ml: float = 0.20             # ML 2x Win Probability (Gradient Boosting)
    weight_delivery: float = 0.08       # Institutional delivery % accumulation
    weight_sentiment: float = 0.07      # Capex, order book, earnings catalysts


    # --- Output & Simulation ---
    top_n: int = 15                     # Top multibaggers to display
    export_csv: bool = False
    simulation_capital: float = 500000.0 # ₹5,00,000 portfolio simulation


DEFAULT_CONFIG = Config()
