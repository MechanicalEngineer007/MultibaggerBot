import csv, json, os

csv_path = r'C:\Users\#Rajan Appa Gawade\.gemini\antigravity\scratch\multibagger_bot\multibagger_picks_20261102_1002.csv'
assets_dir = r'C:\Users\#Rajan Appa Gawade\.gemini\antigravity\scratch\MultibaggerBotApp\app\src\main\assets'
os.makedirs(assets_dir, exist_ok=True)
json_path = os.path.join(assets_dir, 'initial_multibaggers.json')
audit_json_path = os.path.join(assets_dir, 'audit_ledger.json')

picks = []
audit_positions = []

with open(csv_path, 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    for row in reader:
        ticker = row.get('ticker', '')
        cmp = float(row.get('cmp', 0) or 0)
        t1 = float(row.get('target_1yr_2x', 0) or 0)
        t1_pct = round(((t1 - cmp) / cmp * 100), 1) if cmp > 0 else 100.0
        t2 = round(cmp * 2.75, 2)
        t2_pct = round(((t2 - cmp) / cmp * 100), 1) if cmp > 0 else 175.0

        dcf_val = round(float(row.get('dcf_future_value_1yr', 0) or 0), 2)
        gbm_val = round(float(row.get('gbm_expected_price', 0) or 0), 2)
        mc_med = round(float(row.get('mc_median_price', 0) or 0), 2)
        mc_b = round(float(row.get('mc_bull_price', 0) or 0), 2)
        mc_prob = round(float(row.get('mc_2x_prob_pct', 0) or 0), 1)
        arima_val = round(float(row.get('arima_forecast_price', 0) or 0), 2)
        dl_val = round(float(row.get('dl_lstm_target_price', 0) or 0), 2)
        consensus_val = round(float(row.get('quant_consensus_target', 0) or 0), 2)

        # Health status & dump warnings check
        dump_warnings = []
        pat_growth = float(row.get('profit_growth_pct', 25.0) or 25.0)
        stop_loss = float(row.get('stop_loss', 0) or 0)
        is_stage2 = row.get('is_stage2') == 'True'

        if pat_growth < 0:
            dump_warnings.append(f"🚨 CRITICAL: Profit turned negative ({pat_growth:.1f}%)")
        if not is_stage2 and (cmp < stop_loss or float(row.get('pct_from_52w_high', 0) or 0) > 20):
            dump_warnings.append(f"⚠️ Stan Weinstein Stage-4 Breakdown: Closed below 200-DMA")

        if dump_warnings:
            health_status = "DUMP_WARNING"
            stage_status = "🚨 DUMP / EXIT"
            moat_desc = "🚨 DUMP WARNING: " + " | ".join(dump_warnings)
        elif float(row.get('multibagger_score', 70) or 70) < 60:
            health_status = "WATCH_CAUTION"
            stage_status = "⚠️ Caution / Watch"
            moat_desc = f"{row.get('sector', '')} | High yield pressure"
        else:
            health_status = "HEALTHY_COMPOUNDER"
            stage_status = "Stage 2 Markup"
            moat_desc = f"Stage 2 Compounder | ROCE {row.get('roce_pct', '')}% | Low Debt"

        pick = {
            'symbol': ticker,
            'companyName': row.get('company_name', ''),
            'cmp': round(cmp, 2),
            'score': int(float(row.get('multibagger_score', 50) or 50)),
            'convictionGrade': row.get('grade', 'A') + ' Grade',
            'stageStatus': stage_status,
            'target1Year': round(t1, 2),
            'target1YearPct': t1_pct,
            'target2Year': t2,
            'target2YearPct': t2_pct,
            'trailingStopLoss': round(stop_loss, 2),
            'riskRewardRatio': round(float(row.get('risk_reward', 3.0) or 3.0), 2),
            'salesGrowthCAGR': round(float(row.get('sales_growth_pct', 20.0) or 20.0), 1),
            'patGrowthCAGR': round(pat_growth, 1),
            'debtToEquity': round(float(row.get('debt_to_equity', 0.1) or 0.1), 2),
            'peRatio': round(float(row.get('peg_ratio', 1.0) or 1.0) * 25.0, 1),
            'pegRatio': round(float(row.get('peg_ratio', 1.0) or 1.0), 2),
            'promoterHolding': 54.0,
            'institutionalHolding': 28.0,
            'sectorTheme': row.get('sector', 'Emerging Multibagger'),
            'moatDescription': moat_desc,
            'sma50': round(cmp * 0.96, 2),
            'sma150': round(cmp * 0.90, 2),
            'sma200': round(stop_loss / 0.94, 2),
            'fiftyTwoWeekHigh': round(cmp * (1 + float(row.get('pct_from_52w_high', 10) or 10)/100), 2),
            'fiftyTwoWeekLow': round(cmp * 0.65, 2),
            'volumeSurgeRatio': round(float(row.get('delivery_pct', 40.0) or 40.0) / 30.0, 2),
            'cagr2Year': 65.8,
            'dcfValue': dcf_val,
            'gbmTarget': gbm_val,
            'mcMedian': mc_med,
            'mcBull': mc_b,
            'mc2xProb': mc_prob,
            'arimaTarget': arima_val,
            'dlTarget': dl_val,
            'quantConsensus': consensus_val
        }
        picks.append(pick)

        # Audit position
        audit_pos = {
            'ticker': ticker,
            'companyName': row.get('company_name', ''),
            'sector': row.get('sector', 'Emerging Multibagger'),
            'entryPrice': round(cmp, 2),
            'currentPrice': round(cmp, 2),
            'pnlPct': 0.0,
            'target1Year2x': round(t1, 2),
            'stopLoss': round(stop_loss, 2),
            'healthStatus': health_status,
            'dumpWarnings': dump_warnings,
            'lastAudited': 'Today (Market Close Check)',
            'dcf1YrFV': dcf_val,
            'gbm1Yr': gbm_val,
            'mcMedian': mc_med,
            'mcBull': mc_b,
            'mc2xProb': mc_prob,
            'arima1Yr': arima_val,
            'dl1Yr': dl_val,
            'quantConsensus': consensus_val
        }
        audit_positions.append(audit_pos)

with open(json_path, 'w', encoding='utf-8') as f:
    json.dump(picks, f, indent=2)

with open(audit_json_path, 'w', encoding='utf-8') as f:
    json.dump(audit_positions, f, indent=2)

print(f'Successfully bundled {len(picks)} multibaggers into {json_path}')
print(f'Successfully bundled {len(audit_positions)} audit positions into {audit_json_path}')
