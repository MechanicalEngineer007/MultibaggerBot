"""
Macro Regime & Global Liquidity Engine for Multibagger Bot
==========================================================

Tracks the 4 critical macroeconomic pillars governing Indian stock market liquidity & multiples:
1. USD / INR (Dollar vs Rupee Exchange Rate)
2. US Fed Interest Rate / Fed Policy Stance
3. 10-Year Government Bond Yields (US 10Y Treasury & Yield Curve)
4. Crude Oil Prices (Brent / WTI Crude in USD/bbl)

Outputs a calibrated Macro Liquidity Score (0-100), market regime classification,
and sector-specific valuation adjustments.
"""

import os
import json
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Dict, Optional, Tuple

import numpy as np
import pandas as pd
import yfinance as yf

from config import Config, DEFAULT_CONFIG


@dataclass
class MacroEnvironment:
    """Comprehensive snapshot of global macro & Indian market liquidity."""
    # 1. Currency: Dollar vs Rupee
    usd_inr: float = 84.50
    usd_inr_30d_change_pct: float = 0.0
    usd_inr_status: str = "Stable"
    
    # 2. Fed Policy / Interest Rate
    fed_rate_proxy: float = 4.50
    fed_stance: str = "Neutral"
    
    # 3. Bond Yields
    us_10y_yield: float = 4.25
    us_10y_30d_change: float = 0.0
    yield_regime: str = "Moderate"
    
    # 4. Crude Oil
    crude_oil_price: float = 78.50
    crude_30d_change_pct: float = 0.0
    crude_impact_label: str = "Benign"

    # Composite Macro Climate
    macro_score: float = 65.0           # 0 to 100 (100 = Optimal Goldilocks for 2x Multibaggers)
    regime_label: str = "Favorable"     # Goldilocks, Balanced, Headwind
    capital_allocation_pct: float = 100.0 # Recommended capital deployment %
    allocation_guidance: str = ""
    sector_multipliers: Dict[str, float] = field(default_factory=dict)
    headwinds: list = field(default_factory=list)
    tailwinds: list = field(default_factory=list)


class MacroRegimeEngine:
    """Fetches, caches, and models macroeconomic conditions for Indian equities."""

    def __init__(self, cache_dir: str = ".cache", cache_ttl_hours: int = 12):
        self.cache_file = os.path.join(cache_dir, "_macro_regime.json")
        self.cache_ttl_hours = cache_ttl_hours
        os.makedirs(cache_dir, exist_ok=True)

    def get_macro_environment(self, force_refresh: bool = False) -> MacroEnvironment:
        """Fetch macro metrics or return cached snapshot."""
        if not force_refresh and os.path.exists(self.cache_file):
            try:
                with open(self.cache_file, "r", encoding="utf-8") as f:
                    data = json.load(f)
                cached_time = datetime.fromisoformat(data.get("timestamp", ""))
                if (datetime.now() - cached_time).total_seconds() / 3600.0 < self.cache_ttl_hours:
                    # Construct from cache
                    env_dict = data.get("macro_data", {})
                    return MacroEnvironment(**env_dict)
            except Exception:
                pass

        # Live fetch from Yahoo Finance
        env = self._fetch_live_macro()

        # Cache results
        try:
            with open(self.cache_file, "w", encoding="utf-8") as f:
                json.dump({
                    "timestamp": datetime.now().isoformat(),
                    "macro_data": env.__dict__
                }, f, indent=2)
        except Exception:
            pass

        return env

    def _fetch_live_macro(self) -> MacroEnvironment:
        """Download live macro data for USD/INR, Fed Rate proxy, US 10Y Yield, and Crude."""
        # 1. USD/INR
        usd_inr = 84.5
        usd_inr_chg = 0.2
        try:
            inr_df = yf.Ticker("INR=X").history(period="2mo")
            if not inr_df.empty:
                usd_inr = float(inr_df["Close"].iloc[-1])
                start_val = float(inr_df["Close"].iloc[-min(22, len(inr_df))])
                usd_inr_chg = ((usd_inr - start_val) / start_val) * 100.0
        except Exception:
            pass

        # 2. US 10Y Bond Yield (^TNX)
        us_10y = 4.30
        us_10y_chg = 0.0
        try:
            tnx_df = yf.Ticker("^TNX").history(period="2mo")
            if not tnx_df.empty:
                us_10y = float(tnx_df["Close"].iloc[-1])
                start_tnx = float(tnx_df["Close"].iloc[-min(22, len(tnx_df))])
                us_10y_chg = us_10y - start_tnx
        except Exception:
            pass

        # 3. Fed Rate Proxy (^IRX - 13-week T-Bill / Short Yield)
        fed_rate = 4.50
        try:
            irx_df = yf.Ticker("^IRX").history(period="1mo")
            if not irx_df.empty:
                fed_rate = float(irx_df["Close"].iloc[-1])
        except Exception:
            pass

        # 4. Brent Crude Oil (BZ=F or CL=F)
        crude_price = 78.0
        crude_chg = 0.0
        try:
            crude_df = yf.Ticker("BZ=F").history(period="2mo")
            if crude_df.empty:
                crude_df = yf.Ticker("CL=F").history(period="2mo")
            if not crude_df.empty:
                crude_price = float(crude_df["Close"].iloc[-1])
                start_crude = float(crude_df["Close"].iloc[-min(22, len(crude_df))])
                crude_chg = ((crude_price - start_crude) / start_crude) * 100.0
        except Exception:
            pass

        # Compute Macro Score & Regimes
        score = 65.0
        tailwinds = []
        headwinds = []
        sector_multipliers: Dict[str, float] = {}

        # ─── 1. Currency Impact (USD/INR) ───
        if usd_inr_chg > 2.5:
            score -= 10.0
            usd_inr_status = "Rapid Rupee Depreciation (FII Headwind)"
            headwinds.append(f"Sharp Rupee depreciation ({usd_inr:.2f}, +{usd_inr_chg:.1f}% 30d)")
            # Exporters benefit, importers suffer
            sector_multipliers["Defense"] = 1.05
            sector_multipliers["Pharma Formulations"] = 1.06
            sector_multipliers["Automotive Software"] = 1.05
            sector_multipliers["EMS & Electronics"] = 0.95  # import component pressure
        elif usd_inr_chg < -1.0:
            score += 8.0
            usd_inr_status = "Rupee Appreciating / Strong FII Inflows"
            tailwinds.append(f"Strong domestic currency ({usd_inr:.2f})")
        else:
            score += 4.0
            usd_inr_status = f"Stable Rangebound (₹{usd_inr:.2f})"
            tailwinds.append(f"Currency stability (₹{usd_inr:.2f}/$)")

        # ─── 2. Bond Yield Impact (^TNX) ───
        if us_10y >= 4.70:
            score -= 14.0
            yield_regime = "High Yield Spikes (PE Multiple Compression Risk)"
            headwinds.append(f"Elevated US 10Y Yield ({us_10y:.2f}%) compressing growth stock P/E")
        elif us_10y >= 4.30:
            score -= 4.0
            yield_regime = f"Moderate ({us_10y:.2f}%)"
        elif us_10y <= 3.80:
            score += 12.0
            yield_regime = f"Low / Easing ({us_10y:.2f}%) -- Bullish Multiple Expansion"
            tailwinds.append(f"Softening bond yields ({us_10y:.2f}%) sparking liquidity expansion")
        else:
            score += 5.0
            yield_regime = f"Neutral ({us_10y:.2f}%)"

        # ─── 3. Fed Interest Rate Stance ───
        if fed_rate >= 5.0:
            fed_stance = f"Restrictive / Higher-for-Longer ({fed_rate:.2f}%)"
            headwinds.append(f"Restrictive US Federal Reserve interest rates ({fed_rate:.2f}%)")
        elif fed_rate <= 3.75:
            fed_stance = f"Dovish / Easing Cycle ({fed_rate:.2f}%)"
            score += 10.0
            tailwinds.append(f"Global Fed monetary easing cycle ({fed_rate:.2f}%)")
        else:
            fed_stance = f"Neutral / Pivot Watch ({fed_rate:.2f}%)"

        # ─── 4. Crude Oil Impact (Brent) ───
        if crude_price >= 90.0:
            score -= 16.0
            crude_impact_label = f"High Inflationary Danger (${crude_price:.1f}/bbl)"
            headwinds.append(f"Expensive Crude (${crude_price:.1f}/bbl) expanding India CAD & import bill")
            # Severe pressure on paints, chemicals, tires, aviation
            sector_multipliers["Specialty Chemicals"] = 0.90
            sector_multipliers["Renewable Wind"] = 1.08 # Clean energy alternative boost
        elif crude_price >= 82.0:
            score -= 6.0
            crude_impact_label = f"Elevated (${crude_price:.1f}/bbl)"
            headwinds.append(f"Elevated crude costs (${crude_price:.1f}/bbl)")
        elif crude_price <= 72.0:
            score += 15.0
            crude_impact_label = f"Highly Favorable / Disinflationary (${crude_price:.1f}/bbl)"
            tailwinds.append(f"Soft Crude (${crude_price:.1f}/bbl) boosting Indian corporate profit margins")
            sector_multipliers["Specialty Chemicals"] = 1.08
            sector_multipliers["Fast Fashion Retail"] = 1.05
        else:
            score += 6.0
            crude_impact_label = f"Benign / Manageable (${crude_price:.1f}/bbl)"
            tailwinds.append(f"Crude in manageable sweet-spot (${crude_price:.1f}/bbl)")

        final_macro_score = round(min(100.0, max(15.0, score)), 1)

        # Regimes & Advice
        if final_macro_score >= 70.0:
            regime_label = "🌟 GOLDILOCKS / BULLISH TAILWIND"
            capital_allocation_pct = 100.0
            guidance = "Aggressive Deployment: Favorable macro conditions support full small/midcap compounding."
        elif final_macro_score >= 50.0:
            regime_label = "⚖️ BALANCED / SELECTIVE GROWTH"
            capital_allocation_pct = 80.0
            guidance = "Balanced Deployment (80%): Focus on high-ROCE, net cash champions with order book visibility."
        else:
            regime_label = "⚠️ CAUTION / MACRO HEADWINDS"
            capital_allocation_pct = 60.0
            guidance = "Defensive Stance (60%): Prioritize export earners & low PEG stocks; respect 200-DMA trailing stop cushion."

        return MacroEnvironment(
            usd_inr=round(usd_inr, 2),
            usd_inr_30d_change_pct=round(usd_inr_chg, 1),
            usd_inr_status=usd_inr_status,
            fed_rate_proxy=round(fed_rate, 2),
            fed_stance=fed_stance,
            us_10y_yield=round(us_10y, 2),
            us_10y_30d_change=round(us_10y_chg, 2),
            yield_regime=yield_regime,
            crude_oil_price=round(crude_price, 2),
            crude_30d_change_pct=round(crude_chg, 1),
            crude_impact_label=crude_impact_label,
            macro_score=final_macro_score,
            regime_label=regime_label,
            capital_allocation_pct=capital_allocation_pct,
            allocation_guidance=guidance,
            sector_multipliers=sector_multipliers,
            headwinds=headwinds,
            tailwinds=tailwinds
        )
