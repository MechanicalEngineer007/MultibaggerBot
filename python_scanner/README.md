# 🚀 Indian Stock Market Multibagger Bot (Target: 2x in 1 Year)

> [!CAUTION]
> ### ⚠️ PRECAUTIONARY NOTE & IMPORTANT RISK DISCLAIMER
> **This platform is strictly for educational, informational, and research purposes only. It is an algorithmic analytics tool designed to assist with data screening, NOT an actual prediction app or guaranteed forecasting system.**
>
> **Stock market investments are subject to market risks. Historical patterns, statistical models (DCF, GBM, ARIMA, Monte Carlo, Machine Learning), and technical stage setups do not guarantee future performance. Never invest money based solely on algorithmic screening outputs. Always conduct your own independent research, assess your financial risk profile, and consult a SEBI-registered financial advisor before making any investment decisions. Invest at your own risk!**

An institutional-grade algorithmic **Multibagger Stock Discovery & Machine Learning Screener** engineered specifically for the Indian Stock Market (NSE & BSE).

Unlike short-term swing trading (which targets 5%–10% gains over 10–20 days), **Multibagger Bot** is engineered to discover **1-Year 2x Compounders (+100% Return / 2x CMP)** using a confluence of:
1. **Stan Weinstein Stage-2 Structural Markup** & **Mark Minervini 8-Point Trend Template**
2. **Fundamental Moat & Solvency** (High PAT/Sales CAGR, ROCE > 20%, Low Debt / Net Cash, PEG GARP)
3. **Machine Learning 2x Win Probability Ensemble** (Gradient Boosted Ensemble calibrated on historical NSE 2x cycles)
4. **Institutional Delivery Volume Accumulation** (NSE Delivery % & Pocket Pivots)
5. **Capex, Order Book & Catalyst Sentiment** (Automated financial news intelligence)

---

## 🌟 Architecture & Pillars

```
+-------------------------------------------------------------------------------+
|                       INDIAN MULTIBAGGER 2x ENGINE                            |
+-------------------------------------------------------------------------------+
                                        |
       +--------------------------------+-------------------------------+
       |                                |                               |
       v                                v                               v
[ Fundamental Moat ]          [ 💵 DCF Valuation Engine ]      [ ML 2x Ensemble ]
• 3Y PAT CAGR >= 25%          • 2-Stage Cash Flow Model        • Gradient Boosting
• Sales Growth >= 20%         • WACC via CAPM & Beta           • Calibrated 2x Win %
• ROCE >= 20% | ROE >= 18%    • 1-Year DCF Future Value (₹)    • 21+ Engineered Feats
• Debt/Equity <= 0.5 (Cash)   • 5-Year DCF Future Value (₹)    • Top Feature Drivers
• PEG <= 1.5 (GARP)           • Margin of Safety % & DCF Score • Grade (AAA, AA, A, B)
       |                                |                               |
       +--------------------------------+-------------------------------+
                                        |
       +--------------------------------+-------------------------------+
       |                                                                |
       v                                                                v
[ Institutional Delivery & Catalysts ]       [ 🌐 Global Macro Liquidity Regime ]
• Up/Down Volume Accumulation Ratio          • 💵 USD / INR Currency Exchange Rate
• Delivery % Tracker (>= 50%)                • 🏛️ US Fed Interest Rate & Stance
• Order Book & Capex News Scraper            • 📈 10-Year Bond Yields (US 10Y ^TNX)
                                             • 🛢️ Brent Crude Oil ($/bbl)
                                        |
                                        v
                    [ Composite Scorer & Wealth Engine ]
                    • DCF-Anchored 1-Year 2x Target (+100%)
                    • 200-DMA Trailing Stop Floor
                    • Rich Terminal UI & CSV Export
                    • Telegram Alerts & Kite/Groww Links
```



---

## 📋 Installation & Prerequisites

The bot utilizes Python 3.10+ and standard analytical libraries:

```bash
cd "C:\Users\#Rajan Appa Gawade\.gemini\antigravity\scratch\multibagger_bot"
pip install -r requirements.txt
```

*(Note: If you have `swing_bot` installed, the existing virtual environment already has all dependencies ready!)*

---

## 💻 CLI Usage Guide

### 1. Run Default Multibagger Scan (Curated Radar)
```bash
python main.py
```

### 2. Scan Specific Universes (Midcap 150, Smallcap 250, Nifty 500)
```bash
# Scan Nifty Midcap 150
python main.py --universe midcap150 --top 15

# Scan Nifty Smallcap 250
python main.py --universe smallcap250 --top 20

# Scan Full Nifty 500
python main.py --universe nifty500 --top 25
```

### 3. Diagnose Specific Stocks
```bash
python main.py --tickers "BEL,HAL,TRENT,DIXON,SUZLON,KAYNES"
```

### 4. Fast Scan (Skip News Web-Scraping)
```bash
python main.py --no-news
```

### 5. Export Ranked Multibaggers to CSV
```bash
python main.py --top 20 --export
```
Saves results into `multibagger_picks_YYYYMMDD_HHMM.csv`.

### 6. Train / Retrain Machine Learning Model
```bash
python main.py --train-ml
```
Trains the Gradient Boosting ensemble on multi-year historical rolling 252-day forward windows to predict 100%+ gains, outputting accuracy and ROC-AUC metrics.

### 7. Run 1-Year Historical Backtest
```bash
python main.py --backtest
# Or directly:
python backtest.py --universe radar --sample 20
```

### 8. Run Automated Scheduler / Daemon
```bash
# Run scan once and dispatch alerts
python schedule_scanner.py --once

# Run periodic background scanner every 24 hours
python schedule_scanner.py --interval-hours 24
```

---

## 📱 Telegram Alert Bot Setup

1. Open `telegram_config.json`.
2. Add your Telegram Bot Token & Chat ID:
```json
{
    "bot_token": "YOUR_BOT_TOKEN_HERE",
    "chat_id": "YOUR_CHAT_ID_HERE",
    "min_score": 70.0,
    "enabled": true
}
```
3. Run with `--telegram` to broadcast top high-conviction cards:
```bash
python main.py --telegram
```

---

## 📊 Key Indicators & Rules

| Dimension | Metric | Multibagger Criterion |
|---|---|---|
| **PAT Growth** | 3Y Net Profit CAGR | **$\ge 25\%$** YoY acceleration |
| **Sales Growth** | 3Y Revenue CAGR | **$\ge 20\%$** YoY top-line expansion |
| **Capital Efficiency** | ROCE / ROE | **$\ge 20\%$ ROCE**, **$\ge 18\%$ ROE** |
| **Debt Profile** | Debt-to-Equity (D/E) | **$\le 0.4$** (prefer virtually debt-free / net cash) |
| **Valuation** | PEG Ratio | **$0.2 \le \text{PEG} \le 1.5$** (Growth at Reasonable Price) |
| **Trend Template** | Minervini 8-Check | **$\ge 6/8$** (50 > 150 > 200 EMA + Upward 200-DMA) |
| **ATH Proximity** | Distance to 52W High | Within **$20\% - 25\%$** of 52-Week High |
| **52W Low Floor** | Distance from 52W Low | At least **$\ge 30\%$** above 52-Week Low |
| **Stop Cushion** | Trailing Stop-Loss | **200-Day EMA** structural floor (~15–18% max risk) |
| **1-Year Target** | Projected Return | **$+100\%$ ($2\times \text{CMP}$)** with +40% 6-month checkpoint |

---

## ⚠️ Precautionary Note & Disclaimer

> [!WARNING]
> * **Not Financial Advice**: For research and educational purposes only. Multibagger Bot is an algorithmic analytical screening and data processing system; it is **NOT** a registered investment advisor, portfolio manager, or prediction guarantee service.
> * **Capital Risk**: Trading and investing in equities, smallcaps, and midcaps involve substantial risk of monetary loss.
> * **No Guarantee**: Past algorithmic win rates, ML confidence scores, or historical backtests do not guarantee future returns.
> * **Self-Responsibility**: Always conduct independent fundamental and technical due diligence and consult a SEBI-registered financial advisor before executing trades. **Invest at your own risk.**
