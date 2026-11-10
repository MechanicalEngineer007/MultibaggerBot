import json
import urllib.request
import csv
import os

assets_dir = r"C:\Users\#Rajan Appa Gawade\.gemini\antigravity\scratch\MultibaggerBotApp\app\src\main\assets"
audit_file = os.path.join(assets_dir, "audit_ledger.json")
nse_symbols_file = os.path.join(assets_dir, "all_nse_symbols.json")

# 1. Update audit ledger with real live prices
if os.path.exists(audit_file):
    with open(audit_file, "r", encoding="utf-8") as f:
        positions = json.load(f)

    print(f"Updating {len(positions)} audit positions with real live market prices...")
    for p in positions:
        sym = p["ticker"]
        try:
            url = f"https://query2.finance.yahoo.com/v8/finance/chart/{sym}?range=5d&interval=1d"
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=8) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                meta = data["chart"]["result"][0]["meta"]
                cmp = meta.get("regularMarketPrice")
                if cmp:
                    p["currentPrice"] = round(float(cmp), 2)
                    entry = float(p["entryPrice"])
                    p["pnlPct"] = round(((p["currentPrice"] - entry) / entry) * 100.0, 2)
                    
                    # Dump warning logic
                    stop_loss = float(p.get("stopLoss", 0.0) or 0.0)
                    if p["currentPrice"] < stop_loss or p["pnlPct"] <= -15.0:
                        p["healthStatus"] = "DUMP_WARNING"
                        if "Stop-Loss / 200-DMA Breakdown detected" not in p.get("dumpWarnings", []):
                            p["dumpWarnings"] = [f"Closed below trailing stop-loss (₹{stop_loss})", f"P&L dropped to {p['pnlPct']}%"]
                    elif p["pnlPct"] < -5.0:
                        p["healthStatus"] = "WATCH_CAUTION"
                    else:
                        p["healthStatus"] = "HEALTHY_COMPOUNDER"
                    
                    p["lastAudited"] = "Market Verified Live"
                    print(f"  {sym}: Initial={entry} | Current={p['currentPrice']} | PnL={p['pnlPct']}% [{p['healthStatus']}]")
        except Exception as e:
            print(f"  {sym} error: {e}")

    with open(audit_file, "w", encoding="utf-8") as f:
        json.dump(positions, f, indent=2)
    print("Saved updated audit_ledger.json successfully!")

# 2. Fetch and generate full official NSE symbols list
print("\nFetching official NSE Equity Master list...")
nse_url = "https://archives.nseindia.com/content/equities/EQUITY_L.csv"
req = urllib.request.Request(nse_url, headers={"User-Agent": "Mozilla/5.0"})
nse_list = []
try:
    with urllib.request.urlopen(req, timeout=10) as resp:
        lines = [line.decode("utf-8", errors="ignore") for line in resp.readlines()]
        reader = csv.DictReader(lines)
        for row in reader:
            series = row.get(" SERIES", "").strip()
            if series in ("EQ", "BE"):
                raw_sym = row.get("SYMBOL", "").strip()
                company_name = row.get("NAME OF COMPANY", "").strip()
                if raw_sym:
                    nse_list.append({
                        "symbol": f"{raw_sym}.NS",
                        "rawSymbol": raw_sym,
                        "companyName": company_name,
                        "series": series
                    })
    print(f"Successfully processed {len(nse_list)} active NSE equities.")
except Exception as e:
    print(f"Error fetching from NSE: {e}")

if nse_list:
    with open(nse_symbols_file, "w", encoding="utf-8") as f:
        json.dump(nse_list, f, indent=2)
    print(f"Saved {len(nse_list)} NSE symbols to {nse_symbols_file}")
