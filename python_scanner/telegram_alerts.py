"""
Telegram Alert Dispatcher for Multibagger Bot.
Dispatches high-conviction 2x Multibagger cards to private Telegram chats/channels
with 1-click Zerodha Kite and Groww links.
"""

import os
import json
import urllib.parse
from typing import Dict, List, Optional
import requests

from scorer import MultibaggerCandidate
from macro_regime import MacroEnvironment

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
CONFIG_FILE = os.path.join(SCRIPT_DIR, "telegram_config.json")



def load_telegram_config() -> Dict:
    """Loads Telegram config or creates a default template."""
    if os.path.exists(CONFIG_FILE):
        try:
            with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            pass

    default_cfg = {
        "bot_token": "YOUR_BOT_TOKEN_HERE",
        "chat_id": "YOUR_CHAT_ID_HERE",
        "min_score": 70.0,
        "enabled": False
    }

    try:
        with open(CONFIG_FILE, "w", encoding="utf-8") as f:
            json.dump(default_cfg, f, indent=4)
    except Exception:
        pass

    return default_cfg


def format_multibagger_card(c: MultibaggerCandidate, rank: int = 1, macro_env: Optional[MacroEnvironment] = None) -> str:
    """Format rich HTML card for Telegram."""
    clean_ticker = c.ticker.replace(".NS", "")
    kite_url = f"https://kite.zerodha.com/marketwatch"
    groww_url = f"https://groww.in/search?q={clean_ticker}"

    macro_line = ""
    if macro_env is not None:
        macro_line = f"🌐 <b>Macro:</b> {macro_env.regime_label} | Crude: ${macro_env.crude_oil_price:.1f} | USD/INR: ₹{macro_env.usd_inr:.2f} | 10Y: {macro_env.us_10y_yield:.2f}%\n\n"

    card = (
        f"🚀 <b>MULTIBAGGER 2x ALERT #{rank}</b> 🚀\n\n"
        f"💎 <b>Stock:</b> {c.company_name} (<code>{clean_ticker}</code>)\n"
        f"🏭 <b>Sector:</b> {c.sector}\n"
        f"🎖️ <b>Grade:</b> <b>{c.grade}</b> | <b>Score:</b> {c.multibagger_score}/100\n\n"
        f"{macro_line}"
        f"💰 <b>Current Price (CMP):</b> ₹{c.cmp:,.2f}\n"
        f"🎯 <b>1-Year 2x Target:</b> ₹{c.target_1yr_2x:,.2f} (<b>+100%</b>)\n"
        f"🛡️ <b>200-DMA Stop Cushion:</b> ₹{c.stop_loss:,.2f}\n"
        f"⚖️ <b>Risk-to-Reward:</b> 1:{c.risk_reward}\n\n"
        f"💵 <b>DCF Fair Value:</b> ₹{c.dcf_intrinsic_value:,.2f} (MOS: {c.dcf_margin_of_safety_pct:+.1f}%)\n"
        f"🔮 <b>1Y DCF Target:</b> ₹{c.dcf_future_value_1yr:,.2f} | <b>DCF Score:</b> {c.dcf_score:.0f}/100\n\n"
        f"🔬 <b>Individual Model Projections:</b>\n"
        f"• 📈 <b>GBM:</b> ₹{c.gbm_expected_price:,.2f} ({c.gbm_1yr_return_pct:+.1f}%)\n"
        f"• 🎲 <b>Monte Carlo (5k):</b> ₹{c.mc_median_price:,.2f} (2x Prob: <b>{c.mc_2x_prob_pct:.1f}%</b>)\n"
        f"• 📉 <b>ARIMA:</b> ₹{c.arima_forecast_price:,.2f} ({c.arima_return_pct:+.1f}%)\n"
        f"• 🧠 <b>Deep Learning LSTM:</b> ₹{c.dl_lstm_target_price:,.2f} ({c.dl_lstm_return_pct:+.1f}%)\n"
        f"🎯 <b>Consensus Target:</b> <b>₹{c.quant_consensus_target:,.2f}</b> ({c.quant_consensus_return_pct:+.1f}%)\n\n"
        f"🤖 <b>ML 2x Win Probability:</b> <b>{c.ml_2x_prob_pct:.1f}%</b>\n"


        f"📊 <b>Fundamentals:</b>\n"
        f"• PAT Growth: +{c.profit_growth_pct:.0f}% YoY\n"
        f"• ROCE: {c.roce_pct:.0f}% | ROE: {c.roe_pct:.0f}%\n"
        f"• Debt/Equity: {c.debt_to_equity} (Low Risk)\n"
        f"• PEG Ratio: {c.peg_ratio}\n"
        f"📈 <b>Technical:</b> Minervini {c.minervini_score}/8 | Dist to 52W High: -{c.pct_from_52w_high:.1f}%\n\n"
        f"⚡ <a href='{kite_url}'>Open Zerodha Kite</a> | <a href='{groww_url}'>View on Groww</a>\n"
        f"⚠️ <i>Educational research only. Not financial advice.</i>"
    )
    return card



def send_telegram_message(text: str, config: Optional[Dict] = None) -> bool:
    """Send text/HTML message via Telegram Bot API."""
    if config is None:
        config = load_telegram_config()

    bot_token = config.get("bot_token", "")
    chat_id = config.get("chat_id", "")

    if not bot_token or bot_token == "YOUR_BOT_TOKEN_HERE" or not chat_id or chat_id == "YOUR_CHAT_ID_HERE":
        print("[!] Telegram credentials not configured in telegram_config.json")
        return False

    url = f"https://api.telegram.org/bot{bot_token}/sendMessage"
    payload = {
        "chat_id": chat_id,
        "text": text,
        "parse_mode": "HTML",
        "disable_web_page_preview": True
    }

    try:
        resp = requests.post(url, json=payload, timeout=10)
        return resp.status_code == 200
    except Exception as e:
        print(f"[!] Telegram delivery failed: {e}")
        return False


def broadcast_candidates(candidates: List[MultibaggerCandidate], top_n: int = 5, macro_env: Optional[MacroEnvironment] = None) -> int:
    """Broadcast top N multibaggers to Telegram."""
    cfg = load_telegram_config()
    if not cfg.get("enabled", False) and cfg.get("bot_token") == "YOUR_BOT_TOKEN_HERE":
        print("[*] Telegram alerts currently disabled in telegram_config.json.")
        return 0

    sent = 0
    for idx, c in enumerate(candidates[:top_n], 1):
        if c.multibagger_score >= cfg.get("min_score", 65.0):
            card = format_multibagger_card(c, rank=idx, macro_env=macro_env)
            if send_telegram_message(card, cfg):
                sent += 1
    return sent

