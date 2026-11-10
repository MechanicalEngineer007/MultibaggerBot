"""
Composite Scorer for Multibagger Bot.
Combines Fundamental Moat (30%), Stage-2 Minervini Breakout (25%),
Machine Learning 2x Probability (25%), Institutional Delivery (10%),
and News Catalysts (10%) into an institutional-grade Multibagger Score.
"""

from dataclasses import dataclass, field
from typing import Dict, List, Optional

from config import Config, DEFAULT_CONFIG
from macro_regime import MacroEnvironment
from fundamentals import FundamentalResult
from stage_analysis import Stage2Result
from ml_engine import MLPredictionResult
from delivery_tracker import DeliveryResult
from news_sentiment import NewsResult
from sector_rotation import get_sector, get_sector_tailwind_score
from dcf_model import DCFResult
from quantitative_models import QuantitativeModelSuite



@dataclass
class MultibaggerCandidate:
    """A fully evaluated 1-Year 2x Multibagger Candidate."""
    ticker: str
    company_name: str
    sector: str

    # Scores & Ratings
    multibagger_score: float = 0.0     # 0 to 100 composite score
    grade: str = "B"                   # AAA Super Multibagger, AA High Conviction, A Compounder, B
    ml_2x_prob_pct: float = 0.0        # Calibrated ML 2x Win Probability %
    moat_score: float = 0.0            # Fundamental Moat score
    stage2_score: float = 0.0          # Technical / Minervini score
    delivery_score: float = 0.0        # Institutional delivery score
    sentiment_score: float = 0.0       # News & Capex catalyst score

    # Pricing & 2x Targets
    cmp: float = 0.0                   # Current Market Price (₹)
    target_1yr_2x: float = 0.0         # 1-Year 2x Target (+100%) (₹)
    target_6mo: float = 0.0            # 6-Month intermediate (+40%) (₹)
    stop_loss: float = 0.0             # 200-DMA Trailing Structural Floor (₹)
    risk_reward: float = 0.0           # Upside to Downside Ratio (e.g. 5.2x)

    # Fundamental Moat Metrics
    sales_growth_pct: float = 0.0
    profit_growth_pct: float = 0.0
    roce_pct: float = 0.0
    roe_pct: float = 0.0
    debt_to_equity: float = 0.0
    peg_ratio: float = 0.0
    pe_ratio: float = 0.0
    market_cap_cr: float = 0.0

    # DCF Valuation & Future Value Projections
    dcf_score: float = 50.0            # 0 to 100 DCF Valuation Score
    dcf_intrinsic_value: float = 0.0   # Current Fair Value per share (₹)
    dcf_future_value_1yr: float = 0.0  # 1-Year DCF Projected Future Value (₹)
    dcf_future_value_5yr: float = 0.0  # 5-Year DCF Projected Future Value (₹)
    dcf_margin_of_safety_pct: float = 0.0 # Undervaluation / Margin of Safety %
    dcf_verdict: str = "Fair Value"
    dcf_summary: str = ""

    # Quantitative & Statistical Models (Individual Forecasts)
    gbm_expected_price: float = 0.0         # Geometric Brownian Motion 1Y (₹)
    gbm_1yr_return_pct: float = 0.0         # GBM Return %
    mc_median_price: float = 0.0            # Monte Carlo 5,000 Paths 1Y (₹)
    mc_bull_price: float = 0.0              # Monte Carlo 75th percentile (₹)
    mc_2x_prob_pct: float = 0.0             # Monte Carlo 2x Multibagger Probability %
    arima_forecast_price: float = 0.0       # ARIMA 1Y Forecast (₹)
    arima_return_pct: float = 0.0           # ARIMA Return %
    dl_lstm_target_price: float = 0.0       # Deep Learning / LSTM Neural Net 1Y (₹)
    dl_lstm_return_pct: float = 0.0         # Deep Learning Return %
    quant_consensus_target: float = 0.0     # Multi-Model Consensus Target (₹)
    quant_consensus_return_pct: float = 0.0 # Consensus Return %

    # Stage-2 & Technicals


    minervini_score: int = 0           # 0 to 8
    is_stage2: bool = False
    pct_from_52w_high: float = 0.0
    up_down_volume_ratio: float = 0.0
    delivery_pct: float = 0.0

    # Qualitative Catalysts
    sentiment_label: str = "Neutral"
    primary_catalyst: str = ""
    signals: List[str] = field(default_factory=list)
    top_drivers: List[str] = field(default_factory=list)

    # Filter status
    passed_filters: bool = True
    filter_reason: str = "Passed"


def score_candidate(
    ticker: str,
    fund: FundamentalResult,
    stage2: Stage2Result,
    ml_res: MLPredictionResult,
    deliv: Optional[DeliveryResult] = None,
    news: Optional[NewsResult] = None,
    macro_env: Optional[MacroEnvironment] = None,
    dcf_res: Optional[DCFResult] = None,
    quant_suite: Optional[QuantitativeModelSuite] = None,
    config: Config = DEFAULT_CONFIG
) -> MultibaggerCandidate:
    """
    Compute composite 1-Year 2x Multibagger score factoring in DCF Valuation,
    Quantitative Models (GBM, Monte Carlo, ARIMA, Deep Learning), Stage-2 Breakouts,
    and Global Macro Regimes.
    """
    sector = get_sector(ticker)
    sector_tailwind = get_sector_tailwind_score(ticker)

    # Blend ML probability with Monte Carlo 5,000-path stochastic 2x probability
    ml_prob = ml_res.multibagger_prob
    if quant_suite is not None and quant_suite.mc_2x_prob_pct > 0:
        ml_prob = round((0.60 * ml_prob) + (0.40 * quant_suite.mc_2x_prob_pct), 1)

    # Delivery & News fallbacks
    deliv_score = deliv.delivery_score if deliv else 50.0
    deliv_pct = deliv.delivery_pct if deliv else 42.0
    news_score = news.sentiment_score if news else 50.0
    news_label = news.sentiment_label if news else "Neutral"
    key_cat = news.key_catalysts[0] if (news and news.key_catalysts) else ""
    dcf_score_val = dcf_res.dcf_score if dcf_res else 55.0

    # Weighted composite score
    composite = (
        (fund.moat_score * config.weight_fundamentals) +
        (dcf_score_val * config.weight_dcf) +
        (stage2.stage2_score * config.weight_stage2) +
        (ml_res.ml_score * config.weight_ml) +
        (deliv_score * config.weight_delivery) +
        (news_score * config.weight_sentiment)
    )


    # Macro Regime adjustments
    if macro_env is not None:
        # Macro multiplier based on sector specific sensitivity to USD/INR & Crude
        sec_mult = macro_env.sector_multipliers.get(sector, 1.0)
        composite = composite * sec_mult

        # Macro broad regime nudge (-3 to +3 pts)
        macro_nudge = (macro_env.macro_score - 50.0) * 0.06
        composite += macro_nudge

    # Bonus for high-priority sector tailwinds (up to +4 pts)
    if sector_tailwind >= 90.0:
        composite += 3.5
    elif sector_tailwind >= 85.0:
        composite += 2.0

    # Bonus for Stage-2 + Minervini 8/8
    if stage2.minervini_score == 8:
        composite += 4.0
    elif stage2.minervini_score >= 6:
        composite += 2.0

    # Bonus for Net-Cash / Zero Debt + High PAT growth
    if fund.debt_to_equity <= 0.2 and fund.profit_growth_pct >= 25.0:
        composite += 3.0

    # Bonus for Deep DCF Undervaluation (Margin of Safety >= 25%)
    if dcf_res is not None and dcf_res.margin_of_safety_pct >= 25.0:
        composite += 3.5

    final_score = round(min(100.0, max(10.0, composite)), 1)

    # Multibagger Grade Assignment
    if final_score >= 82.0 and ml_res.multibagger_prob >= 65.0:
        grade = "AAA"  # Super Multibagger
    elif final_score >= 70.0:
        grade = "AA"   # High-Conviction Compounder
    elif final_score >= 58.0:
        grade = "A"    # Emerging Growth Leader
    else:
        grade = "B"    # Speculative / Moderate

    # Filters
    passed = True
    reason = "Passed all checks"

    if stage2.cmp < 10.0:
        passed = False
        reason = "Penny stock (CMP < ₹10)"
    elif not fund.passed_filters:
        passed = False
        reason = fund.filter_reason
    elif stage2.cmp < stage2.ema_200 * 0.85:
        passed = False
        reason = "Severe Stage-4 Downtrend below 200 EMA"

    # Merge signals
    signals = list(stage2.signals)
    if fund.grade in ["AAA", "AA"]:
        signals.append(f"Moat Grade {fund.grade} (ROCE {fund.roce_pct}%, D/E {fund.debt_to_equity})")
    if ml_res.high_conviction:
        signals.append(f"ML 2x Win Probability: {ml_res.multibagger_prob}%")

    return MultibaggerCandidate(
        ticker=ticker,
        company_name=fund.company_name,
        sector=sector,
        multibagger_score=final_score,
        grade=grade,
        ml_2x_prob_pct=ml_res.multibagger_prob,
        moat_score=fund.moat_score,
        stage2_score=stage2.stage2_score,
        delivery_score=deliv_score,
        sentiment_score=news_score,
        cmp=stage2.cmp,
        target_1yr_2x=stage2.target_1yr_2x,
        target_6mo=stage2.target_6mo,
        stop_loss=stage2.stop_loss,
        risk_reward=stage2.risk_reward,
        sales_growth_pct=fund.sales_growth_pct,
        profit_growth_pct=fund.profit_growth_pct,
        roce_pct=fund.roce_pct,
        roe_pct=fund.roe_pct,
        debt_to_equity=fund.debt_to_equity,
        peg_ratio=fund.peg_ratio,
        pe_ratio=fund.pe_ratio,
        market_cap_cr=fund.market_cap_cr,
        dcf_score=dcf_res.dcf_score if dcf_res else 55.0,
        dcf_intrinsic_value=dcf_res.dcf_intrinsic_value if dcf_res else 0.0,
        dcf_future_value_1yr=dcf_res.dcf_future_value_1yr if dcf_res else 0.0,
        dcf_future_value_5yr=dcf_res.dcf_future_value_5yr if dcf_res else 0.0,
        dcf_margin_of_safety_pct=dcf_res.margin_of_safety_pct if dcf_res else 0.0,
        dcf_verdict=dcf_res.valuation_verdict if dcf_res else "Fair Value",
        dcf_summary=dcf_res.valuation_summary if dcf_res else "",
        gbm_expected_price=quant_suite.gbm_expected_price if quant_suite else 0.0,
        gbm_1yr_return_pct=quant_suite.gbm_1yr_return_pct if quant_suite else 0.0,
        mc_median_price=quant_suite.mc_median_price if quant_suite else 0.0,
        mc_bull_price=quant_suite.mc_bull_price if quant_suite else 0.0,
        mc_2x_prob_pct=quant_suite.mc_2x_prob_pct if quant_suite else 0.0,
        arima_forecast_price=quant_suite.arima_forecast_price if quant_suite else 0.0,
        arima_return_pct=quant_suite.arima_return_pct if quant_suite else 0.0,
        dl_lstm_target_price=quant_suite.dl_lstm_target_price if quant_suite else 0.0,
        dl_lstm_return_pct=quant_suite.dl_lstm_return_pct if quant_suite else 0.0,
        quant_consensus_target=quant_suite.consensus_quant_target if quant_suite else 0.0,
        quant_consensus_return_pct=quant_suite.consensus_return_pct if quant_suite else 0.0,
        minervini_score=stage2.minervini_score,
        is_stage2=stage2.is_stage2,
        pct_from_52w_high=stage2.pct_from_52w_high,
        up_down_volume_ratio=stage2.up_down_volume_ratio,
        delivery_pct=deliv_pct,
        sentiment_label=news_label,
        primary_catalyst=key_cat,
        signals=signals,
        top_drivers=ml_res.top_drivers,
        passed_filters=passed,
        filter_reason=reason
    )


def rank_candidates(candidates: List[MultibaggerCandidate], top_n: int = 15) -> List[MultibaggerCandidate]:
    """Sort candidates by multibagger score descending and filter."""
    valid = [c for c in candidates if c.passed_filters]
    valid.sort(key=lambda x: (x.multibagger_score, x.ml_2x_prob_pct), reverse=True)
    return valid[:top_n]
