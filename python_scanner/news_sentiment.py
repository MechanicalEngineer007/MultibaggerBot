"""
News Sentiment & Capex Catalyst Engine for Multibagger Bot.
Scrapes Google News RSS for Indian stock market catalysts:
Order wins, capex expansions, earnings surprises, and institutional stake hikes.
"""

import time
import urllib.parse
from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List, Optional
import xml.etree.ElementTree as ET

import requests
from bs4 import BeautifulSoup

from config import Config, DEFAULT_CONFIG


@dataclass
class NewsResult:
    """Output of news sentiment and catalyst analysis."""
    ticker: str
    company_name: str
    sentiment_score: float             # 0 to 100 (50 = Neutral)
    sentiment_label: str               # Bullish, Mildly Bullish, Neutral, Bearish
    catalyst_count: int
    positive_headlines: int
    negative_headlines: int
    headlines: List[str] = field(default_factory=list)
    key_catalysts: List[str] = field(default_factory=list)


# Keywords signalling 1-2 year multibagger triggers
CATALYST_POSITIVE = [
    "order win", "bags order", "secures order", "contract win", "capex",
    "expansion", "capacity", "highest ever", "profit jumps", "pat surges",
    "doubles profit", "guidance upgrade", "fii buy", "promoter buys",
    "breakout", "target raised", "strong results", "all-time high",
    "multibagger", "plis", "defense deal", "export order"
]

CATALYST_NEGATIVE = [
    "probe", "fraud", "raid", "resigns", "auditor quits", "downgrade",
    "profit drops", "loss widens", "default", "pledge", "penalty",
    "sebi", "strike", "plant shutdown", "dispute"
]


def _clean_headline(title: str) -> str:
    """Remove trailing source name from Google News title."""
    if " - " in title:
        parts = title.rsplit(" - ", 1)
        return parts[0].strip()
    return title.strip()


def fetch_news_rss(query: str, max_items: int = 10) -> List[Dict]:
    """Fetch recent news articles via Google News RSS feed."""
    encoded = urllib.parse.quote(query)
    url = f"https://news.google.com/rss/search?q={encoded}&hl=en-IN&gl=IN&ceid=IN:en"
    
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    try:
        resp = requests.get(url, headers=headers, timeout=8)
        if resp.status_code != 200:
            return []

        root = ET.fromstring(resp.content)
        items = []
        for item in root.findall(".//item")[:max_items]:
            title_el = item.find("title")
            pub_el = item.find("pubDate")
            link_el = item.find("link")
            title = title_el.text if title_el is not None else ""
            pub = pub_el.text if pub_el is not None else ""
            link = link_el.text if link_el is not None else ""
            if title:
                items.append({
                    "title": _clean_headline(title),
                    "published": pub,
                    "link": link
                })
        return items
    except Exception:
        return []


def analyze_news(
    ticker: str,
    company_name: str = "",
    config: Config = DEFAULT_CONFIG
) -> NewsResult:
    """Analyze news catalysts for a stock."""
    clean_ticker = ticker.replace(".NS", "").replace("^", "")
    query = f"{clean_ticker} stock India"
    if company_name and company_name != clean_ticker:
        query = f"{company_name} share price"

    articles = fetch_news_rss(query, max_items=config.news_max_headlines)

    pos_count = 0
    neg_count = 0
    catalysts = []
    headline_texts = []

    for art in articles:
        text = art.get("title", "")
        text_lower = text.lower()
        headline_texts.append(text)

        is_pos = False
        for kw in CATALYST_POSITIVE:
            if kw in text_lower:
                pos_count += 1
                is_pos = True
                if len(catalysts) < 3 and text not in catalysts:
                    catalysts.append(text)
                break

        if not is_pos:
            for kw in CATALYST_NEGATIVE:
                if kw in text_lower:
                    neg_count += 1
                    break

    total = pos_count + neg_count
    if total == 0:
        score = 50.0  # Neutral baseline
    else:
        # Scale between 20 and 95
        ratio = (pos_count - neg_count) / max(1, total)
        score = 50.0 + (ratio * 40.0)

    score = round(min(95.0, max(15.0, score)), 1)

    if score >= 65.0:
        label = "Bullish Catalysts"
    elif score >= 55.0:
        label = "Mildly Bullish"
    elif score >= 45.0:
        label = "Neutral"
    else:
        label = "Bearish Headwinds"

    return NewsResult(
        ticker=ticker,
        company_name=company_name or clean_ticker,
        sentiment_score=score,
        sentiment_label=label,
        catalyst_count=len(catalysts),
        positive_headlines=pos_count,
        negative_headlines=neg_count,
        headlines=headline_texts[:5],
        key_catalysts=catalysts
    )


def analyze_batch(
    tickers: List[str],
    config: Config = DEFAULT_CONFIG,
    delay: float = 0.5
) -> Dict[str, NewsResult]:
    """Batch news analysis with respectful rate limiting."""
    results = {}
    for t in tickers:
        results[t] = analyze_news(t, config=config)
        time.sleep(delay)
    return results
