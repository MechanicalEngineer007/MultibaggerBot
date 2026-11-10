import urllib.request
import json

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'}
symbols = 'ICICIBANK.NS,TITAN.NS,BEL.NS,HAL.NS,TRENT.NS,DIXON.NS,KAYNES.NS,SUZLON.NS,POLYCAB.NS,MCX.NS'
url = f'https://query1.finance.yahoo.com/v7/finance/quote?symbols={symbols}'

try:
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req) as resp:
        d = json.loads(resp.read().decode())
        results = d['quoteResponse']['result']
        print(f"Batch quote returned {len(results)} stocks:")
        for r in results:
            print(f"{r['symbol']}: CMP={r.get('regularMarketPrice')}, 52H={r.get('fiftyTwoWeekHigh')}, 52L={r.get('fiftyTwoWeekLow')}, 50DMA={r.get('fiftyDayAverage')}, 200DMA={r.get('twoHundredDayAverage')}")
except Exception as e:
    print('Batch quote error:', e)
