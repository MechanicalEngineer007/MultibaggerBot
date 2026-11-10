"""
Institutional Delivery Tracker for Multibagger Bot.
Tracks high-conviction delivery % accumulation in Indian stocks.
"""

from dataclasses import dataclass
from typing import Dict, Optional
import pandas as pd


@dataclass
class DeliveryResult:
    ticker: str
    delivery_pct: float                # Delivery % (e.g., 55.4%)
    is_institutional_accumulation: bool # Delivery >= 50%
    volume_surge: float                # Today's volume vs 20-DMA volume
    delivery_score: float              # 0 to 100 score


class DeliveryTracker:
    """Tracks institutional volume and delivery accumulation."""

    def __init__(self):
        pass

    def analyze(self, df: pd.DataFrame, ticker: str = "") -> DeliveryResult:
        """
        Estimate delivery accumulation from volume and price action patterns.
        """
        if len(df) < 20:
            return DeliveryResult(
                ticker=ticker,
                delivery_pct=40.0,
                is_institutional_accumulation=False,
                volume_surge=1.0,
                delivery_score=50.0
            )

        vol = df["volume"]
        close = df["close"]
        open_p = df["open"]

        v20_avg = vol.tail(20).mean()
        latest_vol = vol.iloc[-1]
        vol_surge = round(float(latest_vol / max(1.0, v20_avg)), 2)

        # High delivery proxy: days with strong closes near highs with above-average volume
        recent = df.tail(10)
        up_spread_vol = 0.0
        total_vol = recent["volume"].sum()

        for idx, row in recent.iterrows():
            c, o, h, l, v = row["close"], row["open"], row["high"], row["low"], row["volume"]
            rng = max(0.01, h - l)
            close_pos = (c - l) / rng
            if close_pos > 0.6:  # Closed near upper 40% of daily range (institutional buying)
                up_spread_vol += v * close_pos

        est_delivery_pct = round(min(85.0, max(28.0, (up_spread_vol / max(1.0, total_vol)) * 100.0)), 1)
        is_accum = est_delivery_pct >= 50.0 or (vol_surge >= 1.8 and close.iloc[-1] > open_p.iloc[-1])

        # Delivery score 0 - 100
        score = est_delivery_pct * 0.9
        if vol_surge >= 1.5:
            score += 15.0
        delivery_score = round(min(100.0, max(20.0, score)), 1)

        return DeliveryResult(
            ticker=ticker,
            delivery_pct=est_delivery_pct,
            is_institutional_accumulation=is_accum,
            volume_surge=vol_surge,
            delivery_score=delivery_score
        )
