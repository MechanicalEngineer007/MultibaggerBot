import urllib.request
import json

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept-Language': 'en-US,en;q=0.9',
    'Accept': 'application/json, text/plain, */*'
}

opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor())

# 1. Hit homepage to get session cookies
try:
    req_home = urllib.request.Request('https://www.nseindia.com', headers=headers)
    opener.open(req_home)
    print("NSE Homepage cookie established.")

    # 2. Query NIFTY 500 endpoint
    req_api = urllib.request.Request('https://www.nseindia.com/api/equity-stockIndices?index=NIFTY%20500', headers=headers)
    with opener.open(req_api) as resp:
        d = json.loads(resp.read().decode())
        data = d.get('data', [])
        print(f"NSE India returned {len(data)} stocks directly in ONE call!")
        for item in data[:5]:
            print(f"  {item.get('symbol')}: CMP={item.get('lastPrice')}, pChange={item.get('pChange')}%, 52H={item.get('yearHigh')}")
except Exception as e:
    print("NSE API error:", e)
