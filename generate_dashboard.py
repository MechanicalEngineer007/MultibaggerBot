import json, os

assets_dir = r"C:\Users\#Rajan Appa Gawade\.gemini\antigravity\scratch\MultibaggerBotApp\app\src\main\assets"
json_path = os.path.join(assets_dir, "initial_multibaggers.json")

with open(json_path, "r", encoding="utf-8") as f:
    stocks = json.load(f)

html_content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Multibagger Bot - AI & ML Stock Intelligence Dashboard</title>
    <style>
        :root {
            --bg-primary: #0f172a;
            --bg-secondary: #1e293b;
            --bg-card: #1e293b;
            --accent: #38bdf8;
            --accent-green: #22c55e;
            --accent-gold: #f59e0b;
            --accent-red: #ef4444;
            --text-primary: #f8fafc;
            --text-secondary: #94a3b8;
            --border: #334155;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: var(--bg-primary); color: var(--text-primary); padding: 20px; line-height: 1.5; }
        .container { max-width: 1400px; margin: 0 auto; }
        header { text-align: center; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--border); }
        h1 { font-size: 2.2rem; font-weight: 800; background: linear-gradient(135deg, #38bdf8, #818cf8); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .subtitle { color: var(--text-secondary); margin-top: 6px; font-size: 1rem; }
        
        .alert-banner {
            background: rgba(239, 68, 68, 0.15);
            border: 1px solid #ef4444;
            border-left: 6px solid #ef4444;
            padding: 16px 20px;
            border-radius: 8px;
            margin-bottom: 24px;
            color: #fecaca;
        }
        .alert-banner h3 { color: #f87171; margin-bottom: 6px; font-size: 1.1rem; }
        
        .filters { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 20px; align-items: center; }
        .search-input { background: var(--bg-secondary); border: 1px solid var(--border); color: #fff; padding: 10px 16px; border-radius: 8px; min-width: 280px; font-size: 0.95rem; }
        .btn-filter { background: var(--bg-secondary); border: 1px solid var(--border); color: var(--text-secondary); padding: 10px 16px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: 0.2s; }
        .btn-filter.active, .btn-filter:hover { background: #0284c7; color: white; border-color: #0284c7; }
        
        .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 20px; }
        .card { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 20px; transition: transform 0.2s, box-shadow 0.2s; }
        .card:hover { transform: translateY(-3px); box-shadow: 0 10px 25px rgba(0,0,0,0.4); border-color: #475569; }
        .card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
        .symbol { font-size: 1.35rem; font-weight: 800; color: #fff; }
        .company-name { font-size: 0.85rem; color: var(--text-secondary); margin-top: 2px; }
        .badge { padding: 4px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; }
        .badge-aaa { background: rgba(34, 197, 94, 0.2); color: #4ade80; border: 1px solid #22c55e; }
        .badge-aa { background: rgba(56, 189, 248, 0.2); color: #38bdf8; border: 1px solid #0284c7; }
        .badge-a { background: rgba(245, 158, 11, 0.2); color: #fbbf24; border: 1px solid #d97706; }
        
        .price-row { display: flex; justify-content: space-between; align-items: baseline; margin: 12px 0; padding: 10px 0; border-top: 1px solid rgba(255,255,255,0.06); border-bottom: 1px solid rgba(255,255,255,0.06); }
        .cmp { font-size: 1.5rem; font-weight: 800; color: #f8fafc; }
        .targets { text-align: right; }
        .target-1y { color: #4ade80; font-weight: 700; font-size: 0.95rem; }
        .target-2y { color: #38bdf8; font-weight: 700; font-size: 0.85rem; }
        
        .metrics-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin: 14px 0; font-size: 0.85rem; }
        .metric-box { background: rgba(15, 23, 42, 0.6); padding: 8px 12px; border-radius: 6px; }
        .metric-label { color: var(--text-secondary); font-size: 0.75rem; }
        .metric-val { font-weight: 700; color: #e2e8f0; margin-top: 2px; }
        
        .moat-text { font-size: 0.8rem; color: #cbd5e1; background: rgba(30, 41, 59, 0.8); border-left: 3px solid #38bdf8; padding: 8px 12px; border-radius: 4px; margin-top: 12px; }
        
        .btn-kite { display: block; text-align: center; background: #ea580c; color: white; padding: 10px; border-radius: 8px; text-decoration: none; font-weight: 700; margin-top: 14px; transition: 0.2s; }
        .btn-kite:hover { background: #f97316; }
        
        footer { margin-top: 40px; text-align: center; color: var(--text-secondary); font-size: 0.85rem; padding: 20px 0; border-top: 1px solid var(--border); }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>🚀 Multibagger Bot Intelligence Platform</h1>
            <p class="subtitle">Institutional Quantitative Screener • 2-Stage DCF • Minervini Trend Template • ML 2x Ensemble</p>
        </header>

        <div class="alert-banner">
            <h3>⚠️ PRECAUTIONARY NOTE & IMPORTANT DISCLAIMER</h3>
            <p><strong>This application is strictly for educational, informational, and research purposes only. It is an algorithmic decision-support tool, NOT an actual prediction app or guaranteed investment service.</strong></p>
            <p style="margin-top:6px;">Stock market investments are subject to market risks. Past patterns and algorithmic projections (DCF, GBM, ARIMA, ML probabilities) do not guarantee future stock price performance. Never invest funds without independent due diligence. Consult a SEBI-registered financial advisor before placing trades. <strong>Invest at your own risk.</strong></p>
        </div>

        <div class="filters">
            <input type="text" id="searchInput" class="search-input" placeholder="🔍 Search stock (e.g. MCX, BSE, SYRMA)..." onkeyup="filterStocks()">
            <button class="btn-filter active" onclick="setFilter('all', this)">All Stocks</button>
            <button class="btn-filter" onclick="setFilter('AAA', this)">⭐ AAA Prime</button>
            <button class="btn-filter" onclick="setFilter('t1', this)">🎯 1-Yr Target &gt; 35%</button>
            <button class="btn-filter" onclick="setFilter('t2', this)">🚀 2-Yr 2x+ (&gt;100%)</button>
        </div>

        <div class="grid" id="stocksGrid"></div>

        <footer>
            <p>Multibagger Bot — Open Source Quantitative Stock Research Engine • MechanicalEngineer007</p>
            <p style="margin-top:4px; font-size:0.75rem;">Data based on latest calibrated algorithmic run. Invest responsibly.</p>
        </footer>
    </div>

    <script>
        const STOCKS = """ + json.dumps(stocks) + """;
        let activeFilter = 'all';

        function renderStocks() {
            const grid = document.getElementById('stocksGrid');
            const query = document.getElementById('searchInput').value.toUpperCase().trim();
            grid.innerHTML = '';

            const filtered = STOCKS.filter(s => {
                const matchQuery = s.symbol.toUpperCase().includes(query) || (s.companyName && s.companyName.toUpperCase().includes(query));
                if (!matchQuery) return false;
                if (activeFilter === 'AAA') return s.convictionGrade === 'AAA' || s.score >= 80;
                if (activeFilter === 't1') return (s.target1YearPct || 0) >= 35;
                if (activeFilter === 't2') return (s.target2YearPct || 0) >= 100;
                return true;
            });

            if (filtered.length === 0) {
                grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #94a3b8;">No stocks matching current search criteria.</div>';
                return;
            }

            filtered.forEach(s => {
                const card = document.createElement('div');
                card.className = 'card';
                const badgeClass = s.score >= 80 ? 'badge-aaa' : (s.score >= 65 ? 'badge-aa' : 'badge-a');
                
                card.innerHTML = `
                    <div class="card-header">
                        <div>
                            <div class="symbol">${s.symbol}</div>
                            <div class="company-name">${s.companyName || ''}</div>
                        </div>
                        <span class="badge ${badgeClass}">${s.convictionGrade || 'A'} • ${s.score.toFixed(1)}</span>
                    </div>

                    <div class="price-row">
                        <div class="cmp">₹${Number(s.cmp).toLocaleString('en-IN', {maximumFractionDigits: 1})}</div>
                        <div class="targets">
                            <div class="target-1y">1Y: ₹${Number(s.target1Year).toLocaleString('en-IN', {maximumFractionDigits: 1})} (+${s.target1YearPct}%)</div>
                            <div class="target-2y">2Y: ₹${Number(s.target2Year).toLocaleString('en-IN', {maximumFractionDigits: 1})} (+${s.target2YearPct}%)</div>
                        </div>
                    </div>

                    <div class="metrics-grid">
                        <div class="metric-box">
                            <div class="metric-label">Stage-2 Status</div>
                            <div class="metric-val" style="color: ${s.stageStatus ? '#4ade80' : '#fbbf24'}">${s.stageStatus ? '✓ Stage 2 Active' : 'Consolidating'}</div>
                        </div>
                        <div class="metric-box">
                            <div class="metric-label">DCF Intrinsic Val</div>
                            <div class="metric-val">₹${Number(s.dcfValue || 0).toLocaleString('en-IN', {maximumFractionDigits: 1})}</div>
                        </div>
                        <div class="metric-box">
                            <div class="metric-label">Sales CAGR / PAT</div>
                            <div class="metric-val">${s.salesGrowthCAGR}% / ${s.patGrowthCAGR}%</div>
                        </div>
                        <div class="metric-box">
                            <div class="metric-label">Trailing Stop (200DMA)</div>
                            <div class="metric-val" style="color:#f87171">₹${Number(s.trailingStopLoss || 0).toLocaleString('en-IN', {maximumFractionDigits: 1})}</div>
                        </div>
                    </div>

                    ${s.moatDescription ? `<div class="moat-text">💡 <strong>Moat / Catalyst:</strong> ${s.moatDescription}</div>` : ''}

                    <a href="https://kite.zerodha.com/chart/ext/ciq/NSE/${s.symbol.replace('.NS', '')}" target="_blank" class="btn-kite">Open on Zerodha Kite ↗</a>
                `;
                grid.appendChild(card);
            });
        }

        function setFilter(filter, btn) {
            activeFilter = filter;
            document.querySelectorAll('.btn-filter').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            renderStocks();
        }

        function filterStocks() {
            renderStocks();
        }

        renderStocks();
    </script>
</body>
</html>
"""

output_path = r"C:\Users\#Rajan Appa Gawade\.gemini\antigravity\scratch\MultibaggerBotApp\Multibagger_Dashboard.html"
with open(output_path, "w", encoding="utf-8") as f:
    f.write(html_content)

print(f"Generated Dashboard HTML at: {output_path} ({len(html_content)} bytes)")
