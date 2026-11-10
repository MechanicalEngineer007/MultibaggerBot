"""
Fundamental Moat & Solvency Engine for Multibagger Bot.
Evaluates Sales/PAT Growth, ROCE/ROE Capital Efficiency, Low Debt / Cash Rich Balance Sheets,
and PEG Valuation Headroom (GARP).
"""

from dataclasses import dataclass
from typing import Dict, Tuple
from config import Config, DEFAULT_CONFIG


@dataclass
class FundamentalResult:
    """Detailed fundamental analysis output."""
    ticker: str
    company_name: str
    moat_score: float                  # 0 to 100
    grade: str                         # AAA, AA, A, B, C
    sales_growth_pct: float
    profit_growth_pct: float
    roce_pct: float
    roe_pct: float
    debt_to_equity: float
    peg_ratio: float
    pe_ratio: float
    market_cap_cr: float
    operating_margins_pct: float
    passed_filters: bool
    filter_reason: str
    strengths: list
    cautions: list


def analyze_fundamentals(
    fund_data: Dict,
    config: Config = DEFAULT_CONFIG
) -> FundamentalResult:
    """
    Score fundamental moat and solvency on a 100-point institutional scale.
    """
    ticker = fund_data.get("ticker", "")
    company_name = fund_data.get("company_name", ticker.replace(".NS", ""))
    
    sales_growth = float(fund_data.get("sales_growth_pct", 0.0))
    pat_growth = float(fund_data.get("profit_growth_pct", 0.0))
    roce = float(fund_data.get("roce_pct", 0.0))
    roe = float(fund_data.get("roe_pct", 0.0))
    de = float(fund_data.get("debt_to_equity", 0.0))
    peg = float(fund_data.get("peg_ratio", 0.0))
    pe = float(fund_data.get("pe_ratio", 0.0))
    mcap = float(fund_data.get("market_cap_cr", 0.0))
    opm = float(fund_data.get("operating_margins_pct", 0.0))

    score = 0.0
    strengths = []
    cautions = []

    # 1. Sales & Earnings Growth (30 Points)
    growth_pts = 0.0
    if pat_growth >= 30.0:
        growth_pts += 16.0
        strengths.append(f"Explosive PAT growth: +{pat_growth:.1f}%")
    elif pat_growth >= 20.0:
        growth_pts += 12.0
        strengths.append(f"Strong PAT expansion: +{pat_growth:.1f}%")
    elif pat_growth >= 12.0:
        growth_pts += 7.0
    else:
        cautions.append(f"Subdued profit growth: {pat_growth:.1f}%")

    if sales_growth >= 25.0:
        growth_pts += 14.0
        strengths.append(f"Rapid top-line scaling: +{sales_growth:.1f}%")
    elif sales_growth >= 15.0:
        growth_pts += 10.0
        strengths.append(f"Healthy sales CAGR: +{sales_growth:.1f}%")
    elif sales_growth >= 8.0:
        growth_pts += 5.0
    else:
        cautions.append(f"Slow top-line growth: {sales_growth:.1f}%")

    score += growth_pts

    # 2. Capital Efficiency & Operating Moat (25 Points)
    capital_pts = 0.0
    if roce >= 25.0:
        capital_pts += 10.0
        strengths.append(f"Elite ROCE: {roce:.1f}%")
    elif roce >= 18.0:
        capital_pts += 7.5
        strengths.append(f"Superior capital efficiency (ROCE {roce:.1f}%)")
    elif roce >= 12.0:
        capital_pts += 5.0

    if roe >= 20.0:
        capital_pts += 8.0
        strengths.append(f"High ROE compounder: {roe:.1f}%")
    elif roe >= 15.0:
        capital_pts += 6.0
    elif roe >= 10.0:
        capital_pts += 3.5

    if opm >= 22.0:
        capital_pts += 7.0
        strengths.append(f"Pricing power moat (OPM {opm:.1f}%)")
    elif opm >= 14.0:
        capital_pts += 5.0
    elif opm >= 8.0:
        capital_pts += 2.5

    score += capital_pts

    # 3. Balance Sheet Solvency & Debt Health (25 Points)
    debt_pts = 0.0
    if de <= 0.15:
        debt_pts += 25.0
        strengths.append("Virtually Debt-Free / Net Cash Balance Sheet")
    elif de <= 0.40:
        debt_pts += 20.0
        strengths.append(f"Conservative Debt/Equity: {de:.2f}")
    elif de <= 0.75:
        debt_pts += 14.0
    elif de <= 1.10:
        debt_pts += 7.0
        cautions.append(f"Moderate leverage (D/E {de:.2f})")
    else:
        debt_pts += 2.0
        cautions.append(f"Elevated debt burden: D/E {de:.2f}")

    score += debt_pts

    # 4. Valuation Headroom (GARP - Growth At Reasonable Price) (20 Points)
    val_pts = 0.0
    if 0.1 <= peg <= 1.1:
        val_pts += 20.0
        strengths.append(f"Deep Growth-At-A-Discount (PEG {peg:.2f})")
    elif 1.1 < peg <= 1.6:
        val_pts += 15.0
        strengths.append(f"Attractive GARP valuation (PEG {peg:.2f})")
    elif 1.6 < peg <= 2.2:
        val_pts += 10.0
    elif peg > 2.2:
        val_pts += 4.0
        cautions.append(f"Premium multiple (PEG {peg:.2f}, PE {pe:.1f})")
    else:
        # Unknown/negative peg
        val_pts += 8.0

    score += val_pts
    score = min(100.0, max(10.0, score))

    # Grade assignment
    if score >= 85:
        grade = "AAA"
    elif score >= 72:
        grade = "AA"
    elif score >= 60:
        grade = "A"
    elif score >= 45:
        grade = "B"
    else:
        grade = "C"

    # Filter checks
    passed = True
    reason = "Passed all fundamental criteria"

    if de > 1.4:
        passed = False
        reason = f"High Debt-to-Equity ({de:.2f} > 1.4)"
    elif pat_growth < 5.0 and sales_growth < 5.0:
        passed = False
        reason = f"Negative/Stagnant Growth (Sales {sales_growth:.1f}%, PAT {pat_growth:.1f}%)"

    return FundamentalResult(
        ticker=ticker,
        company_name=company_name,
        moat_score=round(score, 1),
        grade=grade,
        sales_growth_pct=round(sales_growth, 1),
        profit_growth_pct=round(pat_growth, 1),
        roce_pct=round(roce, 1),
        roe_pct=round(roe, 1),
        debt_to_equity=round(de, 2),
        peg_ratio=round(peg, 2),
        pe_ratio=round(pe, 1),
        market_cap_cr=round(mcap, 1),
        operating_margins_pct=round(opm, 1),
        passed_filters=passed,
        filter_reason=reason,
        strengths=strengths,
        cautions=cautions
    )
