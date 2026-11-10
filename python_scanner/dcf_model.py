"""
Discounted Cash Flow (DCF) Valuation Engine for Multibagger Bot
===============================================================

Performs institutional 2-stage DCF intrinsic valuation and multi-year future value projections:
1. Stage 1: High growth phase (Years 1 to 5) driven by Sales/PAT CAGR
2. Stage 2: Fade / Transition phase (Years 6 to 10)
3. Terminal Stage: Gordon Growth Model anchored to Indian Long-Term GDP (5.5%)
4. Cost of Capital (WACC / CAPM): Linked to Indian G-Sec / Macro Bond Yields and Beta

Calculates:
- Current DCF Intrinsic Fair Value per share (₹)
- 1-Year Projected DCF Future Value (₹)
- 5-Year Projected DCF Future Value (₹)
- Margin of Safety / Undervaluation %
- Calibrated DCF Valuation Score (0 to 100)
"""

from dataclasses import dataclass
from typing import Dict, Optional, Tuple
from config import Config, DEFAULT_CONFIG
from macro_regime import MacroEnvironment


@dataclass
class DCFResult:
    """Complete Discounted Cash Flow valuation breakdown."""
    ticker: str
    cmp: float                         # Current Market Price (₹)
    fcf_base_cr: float                 # Base Normalized Free Cash Flow (₹ Cr)
    fcf_per_share: float               # FCF per share (₹)
    growth_stage1_pct: float           # Stage 1 Growth Rate % (Years 1-5)
    growth_stage2_pct: float           # Stage 2 Growth Rate % (Years 6-10)
    terminal_growth_pct: float         # Terminal Gordon Growth Rate % (5.5%)
    discount_rate_wacc_pct: float      # Cost of Equity / WACC %
    pv_cash_flows_cr: float            # Present Value of 10-Yr Cash Flows (₹ Cr)
    pv_terminal_value_cr: float        # Present Value of Terminal Value (₹ Cr)
    enterprise_value_cr: float         # Implied Enterprise Value (₹ Cr)
    net_cash_cr: float                 # Cash minus Debt (₹ Cr)
    equity_value_cr: float             # Total Implied Equity Value (₹ Cr)
    shares_outstanding: int            # Share count
    dcf_intrinsic_value: float         # Fair Value per share (₹)
    dcf_future_value_1yr: float        # 1-Year DCF Projected Future Value (₹)
    dcf_future_value_5yr: float        # 5-Year DCF Projected Future Value (₹)
    margin_of_safety_pct: float        # Undervaluation % vs CMP
    dcf_score: float                   # 0 to 100 Valuation Score
    valuation_verdict: str             # Deep Value, Attractive, Fair, Premium
    valuation_summary: str


def calculate_dcf(
    fund_data: Dict,
    macro_env: Optional[MacroEnvironment] = None,
    config: Config = DEFAULT_CONFIG
) -> DCFResult:
    """
    Compute 2-Stage Discounted Cash Flow Valuation.
    """
    ticker = fund_data.get("ticker", "")
    cmp = float(fund_data.get("cmp", 100.0))
    mcap_cr = float(fund_data.get("market_cap_cr", 10000.0))
    shares = int(fund_data.get("shares_outstanding") or (mcap_cr * 1e7 / max(1.0, cmp)))
    shares = max(1000000, shares)

    # 1. Base Free Cash Flow Normalization (₹ Crores)
    fcf_reported = float(fund_data.get("free_cash_flow_cr", 0.0))
    ocf_reported = float(fund_data.get("operating_cash_flow_cr", 0.0))
    pat_growth = float(fund_data.get("profit_growth_pct", 22.0))
    sales_growth = float(fund_data.get("sales_growth_pct", 20.0))
    pe = float(fund_data.get("pe_ratio", 30.0))

    if fcf_reported > 0:
        fcf_base = fcf_reported
    elif ocf_reported > 0:
        fcf_base = ocf_reported * 0.65  # CapEx deduction normalized proxy
    else:
        # Earnings-to-FCF conversion approximation for asset-light compounders
        implied_pat = (mcap_cr / max(5.0, pe)) if pe > 0 else (mcap_cr * 0.04)
        fcf_base = max(10.0, implied_pat * 0.75)

    fcf_per_share = round((fcf_base * 1e7) / shares, 2)

    # 2. Discount Rate (WACC / CAPM)
    # Risk-free rate (India 10Y Benchmark G-Sec proxy ~ 6.9% base)
    rf = 6.90
    if macro_env is not None and macro_env.us_10y_yield > 4.60:
        rf += 0.35  # Global yield spillover premium
    
    beta = float(fund_data.get("beta", 1.05))
    beta = min(1.6, max(0.70, beta))  # Clamp beta
    erp = 5.50  # Equity Risk Premium for India
    
    wacc = rf + (beta * erp)
    wacc_pct = round(wacc, 2)
    wacc_dec = wacc / 100.0

    # 3. Growth Rate Assumptions
    # Stage 1 Growth Rate: blend of PAT CAGR and Sales CAGR, conservatively bounded
    est_growth = (pat_growth * 0.6) + (sales_growth * 0.4)
    g1 = min(32.0, max(14.0, est_growth))  # 14% to 32%
    g1_dec = g1 / 100.0

    # Stage 2 Fade Growth (Years 6-10)
    g_terminal = 5.5  # India long-term nominal GDP rate
    g_terminal_dec = g_terminal / 100.0
    g2 = round((g1 + g_terminal) / 2.0, 1)  # Midpoint fade
    g2_dec = g2 / 100.0

    # 4. Project 10-Year Cash Flows and Discount to Present Value
    pv_cf = 0.0
    curr_fcf = fcf_base

    # Years 1 to 5 (Stage 1)
    for t in range(1, 6):
        curr_fcf = curr_fcf * (1.0 + g1_dec)
        discount_factor = (1.0 + wacc_dec) ** t
        pv_cf += curr_fcf / discount_factor

    fcf_year_5 = curr_fcf

    # Years 6 to 10 (Stage 2 Fade)
    for t in range(6, 11):
        curr_fcf = curr_fcf * (1.0 + g2_dec)
        discount_factor = (1.0 + wacc_dec) ** t
        pv_cf += curr_fcf / discount_factor

    fcf_year_10 = curr_fcf

    # 5. Terminal Value (Gordon Growth Model)
    # Ensure WACC > g_terminal to avoid mathematical divergence
    spread = max(0.02, wacc_dec - g_terminal_dec)
    terminal_value = (fcf_year_10 * (1.0 + g_terminal_dec)) / spread
    pv_tv = terminal_value / ((1.0 + wacc_dec) ** 10)

    # 6. Enterprise Value to Equity Value
    ev = pv_cf + pv_tv
    cash_cr = float(fund_data.get("total_cash_cr", 0.0))
    debt_cr = float(fund_data.get("total_debt_cr", 0.0))
    net_cash = cash_cr - debt_cr
    equity_value = ev + net_cash

    # 7. Per Share Intrinsic Fair Value
    fair_value = (equity_value * 1e7) / shares
    fair_value = round(max(1.0, fair_value), 2)

    # 8. Future Value Projections
    future_value_1yr = round(fair_value * (1.0 + g1_dec), 2)
    future_value_5yr = round(fair_value * ((1.0 + g1_dec) ** 5), 2)

    # 9. Margin of Safety %
    margin_of_safety = round(((fair_value - cmp) / max(1.0, cmp)) * 100.0, 1)

    # 10. Calibrated DCF Valuation Score (0 to 100)
    if margin_of_safety >= 40.0:
        score = 92.0 + min(8.0, (margin_of_safety - 40.0) * 0.2)
        verdict = "Deep Value / Highly Undervalued"
        summary = f"Trading at massive {margin_of_safety:+.1f}% discount to DCF Fair Value (₹{fair_value:,.1f})"
    elif margin_of_safety >= 15.0:
        score = 78.0 + ((margin_of_safety - 15.0) / 25.0) * 14.0
        verdict = "Undervalued / Attractive Entry"
        summary = f"Attractive margin of safety ({margin_of_safety:+.1f}%) with strong compounding headroom"
    elif margin_of_safety >= -10.0:
        score = 62.0 + ((margin_of_safety + 10.0) / 25.0) * 15.0
        verdict = "Fair Value / Quality Compounder"
        summary = f"Trading near intrinsic fair value (₹{fair_value:,.1f}, {margin_of_safety:+.1f}%)"
    elif margin_of_safety >= -35.0:
        score = 45.0 + ((margin_of_safety + 35.0) / 25.0) * 16.0
        verdict = "Growth Premium / Fully Priced"
        summary = f"Priced at premium ({margin_of_safety:+.1f}% vs DCF), requiring sustained high-growth execution"
    else:
        score = max(20.0, 42.0 + ((margin_of_safety + 35.0) * 0.3))
        verdict = "Expensive Multiple / Multiple Risk"
        summary = f"Trades at severe premium ({margin_of_safety:+.1f}% above DCF Fair Value)"

    dcf_score = round(min(100.0, max(20.0, score)), 1)

    return DCFResult(
        ticker=ticker,
        cmp=cmp,
        fcf_base_cr=round(fcf_base, 1),
        fcf_per_share=fcf_per_share,
        growth_stage1_pct=round(g1, 1),
        growth_stage2_pct=round(g2, 1),
        terminal_growth_pct=g_terminal,
        discount_rate_wacc_pct=wacc_pct,
        pv_cash_flows_cr=round(pv_cf, 1),
        pv_terminal_value_cr=round(pv_tv, 1),
        enterprise_value_cr=round(ev, 1),
        net_cash_cr=round(net_cash, 1),
        equity_value_cr=round(equity_value, 1),
        shares_outstanding=shares,
        dcf_intrinsic_value=fair_value,
        dcf_future_value_1yr=future_value_1yr,
        dcf_future_value_5yr=future_value_5yr,
        margin_of_safety_pct=margin_of_safety,
        dcf_score=dcf_score,
        valuation_verdict=verdict,
        valuation_summary=summary
    )
