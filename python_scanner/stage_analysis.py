"""
Stage-2 Growth Breakout Engine for Multibagger Bot.
Implements Mark Minervini's Official 8-Point Trend Template and Stan Weinstein's
Stage-2 Structural Accumulation Analysis.
"""

from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple
import numpy as np
import pandas as pd
from ta.trend import EMAIndicator, SMAIndicator
from ta.volatility import AverageTrueRange

from config import Config, DEFAULT_CONFIG


@dataclass
class Stage2Result:
    """Output of Stage-2 Technical & Trend Template Analysis."""
    ticker: str
    cmp: float
    target_1yr_2x: float               # 2x Target Price (+100%)
    target_6mo: float                  # 6-Month intermediate target (+40%)
    stop_loss: float                   # 200-DMA Trailing Cushion
    risk_reward: float                 # Target upside vs Downside cushion
    
    # Trend Template checks
    is_stage2: bool                    # Weinstein Stage-2 confirmed
    minervini_score: int               # 0 to 8 checks passed
    minervini_passed: bool             # >= 6 checks passed
    
    # Moving Averages
    ema_50: float
    ema_150: float
    ema_200: float
    dma_200_slope_positive: bool
    
    # 52-Week Statistics
    high_52w: float
    low_52w: float
    pct_from_52w_high: float
    pct_above_52w_low: float
    
    # Relative Strength & Volume Accumulation
    rs_nifty_score: float              # Mansfield Relative Strength vs NIFTY
    up_down_volume_ratio: float        # Institutional accumulation ratio
    pocket_pivots: int                 # Pocket pivot count in last 30 days
    
    # Technical Score (0-100)
    stage2_score: float
    signals: List[str] = field(default_factory=list)


def analyze_stage2(
    df: pd.DataFrame,
    benchmark_df: Optional[pd.DataFrame] = None,
    config: Config = DEFAULT_CONFIG,
    ticker: str = ""
) -> Stage2Result:
    """
    Run full Minervini Trend Template and Weinstein Stage-2 technical diagnosis.
    """
    close = df["close"]
    high = df["high"]
    low = df["low"]
    volume = df["volume"]
    cmp = float(close.iloc[-1])

    # 1. Moving Averages
    ema_50 = float(EMAIndicator(close=close, window=50).ema_indicator().iloc[-1])
    ema_150 = float(EMAIndicator(close=close, window=150).ema_indicator().iloc[-1])
    ema_200_series = EMAIndicator(close=close, window=200).ema_indicator()
    ema_200 = float(ema_200_series.iloc[-1])

    # Check 200-day slope over lookback
    slope_window = min(len(ema_200_series) - 1, config.dma200_slope_lookback)
    dma_200_prior = float(ema_200_series.iloc[-slope_window])
    dma_200_slope_positive = ema_200 >= dma_200_prior

    # 2. 52-Week High & Low
    lookback_52w = min(len(df), 252)
    high_52w = float(high.tail(lookback_52w).max())
    low_52w = float(low.tail(lookback_52w).min())

    pct_from_52w_high = max(0.0, ((high_52w - cmp) / high_52w) * 100.0)
    pct_above_52w_low = max(0.0, ((cmp - low_52w) / low_52w) * 100.0)

    # 3. Minervini 8 Criteria Checklist
    checks = []
    # Check 1: Price > 150 EMA and Price > 200 EMA
    c1 = (cmp > ema_150) and (cmp > ema_200)
    checks.append(c1)
    # Check 2: 150 EMA > 200 EMA
    c2 = ema_150 > ema_200
    checks.append(c2)
    # Check 3: 200 EMA is trending up
    c3 = dma_200_slope_positive
    checks.append(c3)
    # Check 4: 50 EMA > 150 EMA and 50 EMA > 200 EMA
    c4 = (ema_50 > ema_150) and (ema_50 > ema_200)
    checks.append(c4)
    # Check 5: Price > 50 EMA
    c5 = cmp > ema_50
    checks.append(c5)
    # Check 6: Price >= 30% above 52-Week Low
    c6 = pct_above_52w_low >= config.min_pct_above_52w_low
    checks.append(c6)
    # Check 7: Price within 25% of 52-Week High
    c7 = pct_from_52w_high <= config.max_pct_from_52w_high
    checks.append(c7)

    # 4. Mansfield Relative Strength vs Nifty 50
    rs_score = 50.0
    if benchmark_df is not None and len(benchmark_df) >= 60:
        try:
            # Align dates
            aligned = pd.concat([close.rename("stock"), benchmark_df["close"].rename("nifty")], axis=1).dropna()
            if len(aligned) >= 50:
                stock_ret = (aligned["stock"].iloc[-1] / aligned["stock"].iloc[-50]) - 1.0
                nifty_ret = (aligned["nifty"].iloc[-1] / aligned["nifty"].iloc[-50]) - 1.0
                rel_diff = (stock_ret - nifty_ret) * 100.0
                rs_score = round(min(100.0, max(0.0, 50.0 + rel_diff * 1.5)), 1)
        except Exception:
            pass

    # Check 8: RS Outperformance
    c8 = rs_score >= 50.0
    checks.append(c8)

    minervini_score = sum(1 for c in checks if c)
    minervini_passed = minervini_score >= 6
    is_stage2 = c1 and c2 and c3 and c4

    # 5. Institutional Volume Footprint (Up/Down Volume Ratio & Pocket Pivots)
    up_down_ratio = 1.0
    pocket_pivots = 0
    if len(df) >= 30:
        tail_df = df.tail(30)
        up_vol = tail_df[tail_df["close"] >= tail_df["open"]]["volume"].sum()
        down_vol = tail_df[tail_df["close"] < tail_df["open"]]["volume"].sum()
        up_down_ratio = round(up_vol / max(1.0, down_vol), 2)

        # Count pocket pivots (up-day volume > 10-day max down-volume)
        for i in range(10, len(tail_df)):
            curr_row = tail_df.iloc[i]
            if curr_row["close"] > curr_row["open"]:
                prev_10 = tail_df.iloc[i-10:i]
                down_days = prev_10[prev_10["close"] < prev_10["open"]]
                max_down_vol = down_days["volume"].max() if not down_days.empty else 0
                if curr_row["volume"] > max_down_vol and max_down_vol > 0:
                    pocket_pivots += 1

    # 6. Composite Stage-2 Score (0 - 100)
    score = (minervini_score / 8.0) * 55.0  # Up to 55 pts
    score += (rs_score / 100.0) * 25.0       # Up to 25 pts
    
    # Volume accumulation bonus (up to 20 pts)
    vol_pts = min(20.0, up_down_ratio * 10.0)
    score += vol_pts
    stage2_score = round(min(100.0, max(15.0, score)), 1)

    # 7. Targets and Stop Loss Levels
    target_1yr_2x = round(cmp * (1.0 + (config.target_return_pct / 100.0)), 2)
    target_6mo = round(cmp * (1.0 + (config.intermediate_target_pct / 100.0)), 2)
    
    # Trailing Stop Cushion: EMA 200 or -18% buffer, whichever is tighter
    cushion_ema200 = round(ema_200 * 0.97, 2)  # 3% below 200 EMA to avoid whipsaws
    cushion_fixed = round(cmp * (1.0 - (config.trailing_sl_pct / 100.0)), 2)
    stop_loss = max(cushion_ema200, cushion_fixed)
    if stop_loss >= cmp:
        stop_loss = round(cmp * 0.85, 2)

    risk = max(1.0, cmp - stop_loss)
    reward = target_1yr_2x - cmp
    risk_reward = round(reward / risk, 2)

    signals = []
    if is_stage2:
        signals.append("Weinstein Stage-2 Markup Phase")
    if minervini_passed:
        signals.append(f"Minervini Trend Template ({minervini_score}/8)")
    if pct_from_52w_high <= 10.0:
        signals.append("Near 52-Week High Breakout Zone")
    if up_down_ratio >= 1.5:
        signals.append(f"Institutional Accumulation (Up/Down Vol {up_down_ratio}x)")
    if rs_score >= 65.0:
        signals.append("High Relative Strength vs NIFTY 50")

    return Stage2Result(
        ticker=ticker,
        cmp=cmp,
        target_1yr_2x=target_1yr_2x,
        target_6mo=target_6mo,
        stop_loss=stop_loss,
        risk_reward=risk_reward,
        is_stage2=is_stage2,
        minervini_score=minervini_score,
        minervini_passed=minervini_passed,
        ema_50=round(ema_50, 2),
        ema_150=round(ema_150, 2),
        ema_200=round(ema_200, 2),
        dma_200_slope_positive=dma_200_slope_positive,
        high_52w=round(high_52w, 2),
        low_52w=round(low_52w, 2),
        pct_from_52w_high=round(pct_from_52w_high, 1),
        pct_above_52w_low=round(pct_above_52w_low, 1),
        rs_nifty_score=rs_score,
        up_down_volume_ratio=up_down_ratio,
        pocket_pivots=pocket_pivots,
        stage2_score=stage2_score,
        signals=signals
    )
