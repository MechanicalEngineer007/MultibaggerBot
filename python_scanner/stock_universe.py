"""
Stock Universe Provider for Multibagger Bot.
Provides curated multibagger radar, Nifty Midcap 150, Smallcap 250, and Nifty 500 tickers.
All tickers include the '.NS' suffix for yfinance compatibility.
"""

from typing import List, Dict
from config import UNIVERSE_RADAR, UNIVERSE_MIDCAP150, UNIVERSE_SMALLCAP250, UNIVERSE_NIFTY500, UNIVERSE_CUSTOM

# ─── Curated Multibagger Radar Tickers (High-Beta Growth Champions) ───────────

MULTIBAGGER_RADAR = [
    # Defense & Aerospace
    "BEL", "HAL", "BDL", "COCHINSHIP", "SOLARINDS", "DATAPATTNS",
    # Electronics Manufacturing Services (EMS)
    "DIXON", "KAYNES", "SYRMA", "AMBER",
    # Green Energy, Power T&D & Wind
    "SUZLON", "INOXWIND", "CGPOWER", "JSWENERGY", "TATAPOWER",
    # Railways, Infrastructure & Cables
    "TITAGARH", "IRCON", "RVNL", "RAILTEL", "POLYCAB", "KEI",
    # Capital Goods & Engineering
    "CUMMINSIND", "BHEL", "ABB", "SIEMENS",
    # High-Growth Retail & Consumer
    "TRENT", "KALYANKJIL", "ZOMATO", "BIKAJI",
    # Capital Markets, FinTech & Exchanges
    "BSE", "CDSL", "MOTILALOFS", "ANGELONE", "MCX",
    # Emerging Tech, ER&D and AI Enablers
    "KPITTECH", "PERSISTENT", "COFORGE", "NETWEB",
    # Auto EV & Clean Mobility
    "OLECTRA", "MOTHERSON", "TIINDIA", "JBMA",
    # High ROCE Pharma & Specialty Chemicals
    "MANKIND", "TORNTPHARM", "DEEPAKNTR", "PIIND",
]

# ─── Nifty Midcap 150 Selection ───────────────────────────────────────────────

NIFTY_MIDCAP_150 = [
    "ABB", "ABCAPITAL", "ALKYLAMINE", "APLAPOLLO", "ASTRAL",
    "ATUL", "AUBANK", "AUROPHARMA", "BALKRISIND", "BANDHANBNK",
    "BANKBARODA", "BATAINDIA", "BDL", "BEL", "BHARATFORG",
    "BHEL", "BIOCON", "BOSCHLTD", "BSE", "CAMS",
    "CANFINHOME", "CGPOWER", "CHOLAFIN", "CLEAN", "COCHINSHIP",
    "COFORGE", "COLPAL", "CONCOR", "COROMANDEL", "CROMPTON",
    "CUMMINSIND", "DALBHARAT", "DATAPATTNS", "DEEPAKNTR", "DELHIVERY",
    "DIXON", "ESCORTS", "EXIDEIND", "FEDERALBNK", "FORTIS",
    "GLENMARK", "GMRAIRPORT", "GODREJPROP", "GUJGASLTD", "HAL",
    "HDFCAMC", "HINDPETRO", "IDFCFIRSTB", "INDHOTEL", "INDIAMART",
    "INDUSTOWER", "IPCALAB", "IRCTC", "IRFC", "JBCHEPHARM",
    "JINDALSTEL", "JSWENERGY", "JUBLFOOD", "KALYANKJIL", "KAYNES",
    "KEI", "KPITTECH", "LALPATHLAB", "LAURUSLABS", "LICHSGFIN",
    "LODHA", "LUPIN", "MANKIND", "MARICO", "MAXHEALTH",
    "MCX", "MFSL", "MOTHERSON", "MOTILALOFS", "MPHASIS",
    "MRF", "MUTHOOTFIN", "NAUKRI", "NAVINFLUOR", "NHPC",
    "NMDC", "OBEROIRLTY", "OFSS", "PAGEIND", "PERSISTENT",
    "PETRONET", "PFC", "PHOENIXLTD", "PIDILITIND", "PIIND",
    "PNB", "POLYCAB", "POONAWALLA", "PRESTIGE", "PVRINOX",
    "RAMCOCEM", "RECLTD", "SAIL", "SBICARD", "SCHAEFFLER",
    "SJVN", "SOLARINDS", "SONACOMS", "SRF", "SUNDARMFIN",
    "SUPREMEIND", "SUZLON", "SYNGENE", "TATACOMM", "TATAELXSI",
    "TATAPOWER", "TATATECH", "TIINDIA", "TITAGARH", "TORNTPHARM",
    "TORNTPOWER", "TRENT", "UBL", "UNIONBANK", "UNITDSPR",
    "VBL", "VOLTAS", "YESBANK", "ZOMATO", "ZYDUSLIFE",
]

# ─── Nifty Smallcap 250 Selection ─────────────────────────────────────────────

NIFTY_SMALLCAP_250 = [
    "AARTIIND", "AFFLE", "AJANTPHARM", "AMBER", "ANGELONE",
    "ANURAS", "APTUS", "ASAHIINDIA", "AVALON", "AVANTIFEED",
    "BAJAJELEC", "BALRAMCHIN", "BECTORFOOD", "BIKAJI", "BLUESTARCO",
    "BSOFT", "CAMPUS", "CANBK", "CARBORUNIV", "CASTROLIND",
    "CEATLTD", "CENTRALBK", "CENTURYPLY", "CENTURYTEX", "CESC",
    "CHALET", "CHAMBLFERT", "CREDITACC", "CYIENT", "DATAPATTNS",
    "DCMSHRIRAM", "DEVYANI", "ECLERX", "ELGIEQUIP", "EMAMILTD",
    "ENGINERSIN", "EQUITASBNK", "ERIS", "FINCABLES", "FINPIPE",
    "FIVESTAR", "FLUOROCHEM", "GLS", "GNFC", "GODFRYPHLP",
    "GRINDWELL", "GSFC", "HAPPSTMNDS", "HATSUN", "HEG",
    "HFCL", "HIKAL", "IDBI", "IIFL", "INDIACEM",
    "INOXWIND", "INTELLECT", "IONEXCHANG", "IRCON", "ITI",
    "JAMNAAUTO", "JBMA", "JINDALSAW", "JKCEMENT", "JKLAKSHMI",
    "JKPAPER", "JMFINANCIL", "JSL", "JTEKTINDIA", "JUSTDIAL",
    "JYOTHYLAB", "KEC", "KFINTECH", "KIMS", "KNRCON",
    "KRBL", "LATENTVIEW", "LEMONTREE", "LINDEINDIA", "LLOYDSME",
    "LTFOODS", "LUXIND", "MANAPPURAM", "MAPMYINDIA", "MARKSANS",
    "MASTEK", "MEDANTA", "METROPOLIS", "MHRIL", "MINDACORP",
    "MMTC", "MSTCLTD", "MTARTECH", "NAM-INDIA", "NATCOPHARM",
    "NATIONALUM", "NAVA", "NCC", "NETWORK18", "NLCINDIA",
    "OLECTRA", "PRAJIND", "PRINCEPIPE", "RBLBANK", "RITES",
    "SAFARI", "SANSERA", "SHOPERSTOP", "SONATSOFTW", "STARHEALTH",
    "SYRMA", "TANLA", "TEJASNET", "TRITURBINE", "VAIBHAVGBL",
    "WELCORP", "WELSPUNLIV", "WESTLIFE", "ZENSARTECH",
]

# ─── Sector Mapping for Multibagger Themes ────────────────────────────────────

SECTOR_MAP: Dict[str, str] = {
    # Defense
    "BEL": "Defense", "HAL": "Defense", "BDL": "Defense", "COCHINSHIP": "Defense",
    "SOLARINDS": "Defense & Explosives", "DATAPATTNS": "Defense & Aerospace",
    # EMS
    "DIXON": "EMS & Electronics", "KAYNES": "EMS & Electronics", "SYRMA": "EMS & Electronics", "AMBER": "EMS & HVAC",
    # Energy
    "SUZLON": "Renewable Wind", "INOXWIND": "Renewable Wind", "CGPOWER": "Power Equipment",
    "JSWENERGY": "Power Generation", "TATAPOWER": "Integrated Power",
    # Rails & Infra
    "TITAGARH": "Railways & Wagons", "IRCON": "Railways EPC", "RVNL": "Railways EPC", "RAILTEL": "Rail Telecom",
    "POLYCAB": "Cables & Wires", "KEI": "Cables & Wires",
    # Retail & Consumer
    "TRENT": "Fast Fashion Retail", "KALYANKJIL": "Jewellery Retail", "ZOMATO": "FoodTech & Blinkit", "BIKAJI": "Packaged Foods",
    # Capital Markets
    "BSE": "Stock Exchange", "CDSL": "Depository", "MOTILALOFS": "Wealth Management", "ANGELONE": "Retail Brokerage", "MCX": "Commodity Exchange",
    # Tech
    "KPITTECH": "Automotive Software", "PERSISTENT": "Cloud & AI Services", "COFORGE": "Enterprise IT", "NETWEB": "High-End Supercomputing",
    # Auto
    "OLECTRA": "EV Buses", "MOTHERSON": "Auto Components", "TIINDIA": "Engineering & Mobility", "JBMA": "EV Buses & Components",
    # Pharma/Chem
    "MANKIND": "Pharma & OTC", "TORNTPHARM": "Pharma Formulations", "DEEPAKNTR": "Specialty Chemicals", "PIIND": "Agrochem & CSM",
}


def get_tickers(universe: str, custom_list: List[str] = None) -> List[str]:
    """
    Get a clean list of NSE stock tickers with .NS suffix.
    """
    if universe == UNIVERSE_RADAR:
        raw = MULTIBAGGER_RADAR
    elif universe == UNIVERSE_MIDCAP150:
        raw = NIFTY_MIDCAP_150
    elif universe == UNIVERSE_SMALLCAP250:
        raw = NIFTY_SMALLCAP_250
    elif universe == UNIVERSE_NIFTY500:
        # Union of radar, midcap 150, and smallcap 250
        seen = set()
        raw = []
        for t in MULTIBAGGER_RADAR + NIFTY_MIDCAP_150 + NIFTY_SMALLCAP_250:
            if t not in seen:
                seen.add(t)
                raw.append(t)
    elif universe == UNIVERSE_CUSTOM:
        raw = custom_list or []
    else:
        raw = MULTIBAGGER_RADAR

    # Format cleanly with .NS suffix
    clean_tickers = []
    for ticker in raw:
        t = ticker.strip().upper().replace(" ", "")
        if not t.endswith(".NS"):
            t = f"{t}.NS"
        if t not in clean_tickers:
            clean_tickers.append(t)

    return clean_tickers


def get_sector(ticker: str) -> str:
    """Return mapped sector or general sector classification."""
    clean = ticker.replace(".NS", "").upper()
    return SECTOR_MAP.get(clean, "Growth & Manufacturing")
