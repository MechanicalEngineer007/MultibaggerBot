"""
Sector Tailwinds & Theme Engine for Multibagger Bot.
Maps high-growth thematic megatrends in India (Make in India, Defense, EMS, Power T&D, Railways).
"""

from typing import Dict, List
import pandas as pd
from stock_universe import SECTOR_MAP, get_sector


THEME_TAILWIND_RATINGS: Dict[str, float] = {
    "Defense": 95.0,
    "Defense & Aerospace": 95.0,
    "Defense & Explosives": 92.0,
    "EMS & Electronics": 94.0,
    "EMS & HVAC": 88.0,
    "Renewable Wind": 92.0,
    "Power Equipment": 90.0,
    "Power Generation": 85.0,
    "Railways & Wagons": 90.0,
    "Railways EPC": 86.0,
    "Cables & Wires": 88.0,
    "Fast Fashion Retail": 88.0,
    "Jewellery Retail": 85.0,
    "Stock Exchange": 89.0,
    "Depository": 87.0,
    "Automotive Software": 86.0,
    "High-End Supercomputing": 92.0,
    "EV Buses": 87.0,
    "Specialty Chemicals": 82.0,
    "Pharma Formulations": 80.0,
}


def get_sector_tailwind_score(ticker: str) -> float:
    """Return thematic tailwind score (0-100) based on sector alignment."""
    sector = get_sector(ticker)
    return THEME_TAILWIND_RATINGS.get(sector, 75.0)
