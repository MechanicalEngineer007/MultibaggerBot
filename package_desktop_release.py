import os
import shutil
import zipfile

root_dir = r"C:\Users\#Rajan Appa Gawade\.gemini\antigravity\scratch\MultibaggerBotApp"
python_src = r"C:\Users\#Rajan Appa Gawade\.gemini\antigravity\scratch\multibagger_bot"
desktop_apk = r"C:\Users\#Rajan Appa Gawade\Desktop\MultibaggerBot.apk"

release_dir = os.path.join(root_dir, "release_builds")
desktop_bundle_dir = os.path.join(release_dir, "MultibaggerBot-Desktop-v1.0.0")

os.makedirs(desktop_bundle_dir, exist_ok=True)
os.makedirs(release_dir, exist_ok=True)

# 1. Copy Android APK to release_builds
apk_dest = os.path.join(release_dir, "MultibaggerBot.apk")
shutil.copy2(desktop_apk, apk_dest)
print(f"Copied APK to release_builds: {os.path.getsize(apk_dest)} bytes")

# 2. Add files to desktop bundle
shutil.copy2(os.path.join(root_dir, "Multibagger_Dashboard.html"), os.path.join(desktop_bundle_dir, "Multibagger_Dashboard.html"))

# Copy python scanner files
for item in os.listdir(python_src):
    if item in [".cache", "__pycache__", ".venv"]:
        continue
    s = os.path.join(python_src, item)
    d = os.path.join(desktop_bundle_dir, item)
    if os.path.isdir(s):
        shutil.copytree(s, d, dirs_exist_ok=True)
    else:
        shutil.copy2(s, d)

# Write Launcher .bat
launcher_content = """@echo off
title Multibagger Bot - Desktop Suite
cls
echo =========================================================================
echo    Multibagger Bot - AI & ML Stock Discovery Engine (Desktop Suite)
echo =========================================================================
echo.
echo [!] PRECAUTIONARY NOTE:
echo This software is strictly for research and data-screening assistance.
echo It is NOT an actual prediction app or investment advisory tool.
echo Equities carry high capital risk. Invest at your own risk!
echo.
echo -------------------------------------------------------------------------
echo Select an option:
echo   [1] Open Offline Interactive Multibagger Dashboard (Web Browser)
echo   [2] Run Live Algorithmic Screener (Python CLI)
echo   [3] Run 1-Year Historical Backtest
echo   [4] Retrain Machine Learning 2x Model
echo   [5] Exit
echo -------------------------------------------------------------------------
set /p choice="Enter choice [1-5]: "

if "%choice%"=="1" (
    start "" "Multibagger_Dashboard.html"
    exit /b
)
if "%choice%"=="2" (
    python main.py
    pause
    exit /b
)
if "%choice%"=="3" (
    python main.py --backtest
    pause
    exit /b
)
if "%choice%"=="4" (
    python main.py --train-ml
    pause
    exit /b
)
exit /b
"""
with open(os.path.join(desktop_bundle_dir, "Launch_Multibagger_Desktop.bat"), "w", encoding="utf-8") as f:
    f.write(launcher_content)

# Write README.txt
readme_txt = """=========================================================================
  MULTIBAGGER BOT — DESKTOP SUITE v1.0.0 (Windows / Portable)
=========================================================================

⚠️ PRECAUTIONARY NOTE & IMPORTANT DISCLAIMER:
This software is strictly for educational, research, and algorithmic data-screening assistance only.
It is NOT an actual prediction application, registered investment advisor, or guaranteed wealth generator.

Stock market trading and investments in equities (NSE & BSE) carry significant risk of capital loss.
Statistical models (DCF, GBM, ARIMA, Monte Carlo simulations) and Machine Learning probabilities
are calculated from historical financial metrics and DO NOT guarantee future price movements.
Always perform independent due diligence and consult a SEBI-registered financial advisor.
INVEST AT YOUR OWN RISK.

-------------------------------------------------------------------------
HOW TO USE THIS DESKTOP SUITE:
-------------------------------------------------------------------------
1. INSTANT OFFLINE DASHBOARD:
   Double-click "Multibagger_Dashboard.html" to open the interactive
   stock screener, Stage-2 setups, and DCF targets directly in any browser.

2. ONE-CLICK LAUNCHER:
   Double-click "Launch_Multibagger_Desktop.bat" to easily launch the dashboard,
   run live stock universe scans, or execute backtests.

3. PYTHON CLI USAGE:
   Install prerequisites:
     pip install -r requirements.txt
   Run scan:
     python main.py
   Scan custom universe:
     python main.py --universe midcap150 --top 15
   Export results:
     python main.py --top 20 --export

-------------------------------------------------------------------------
MOBILE ANDROID APP:
-------------------------------------------------------------------------
If you also want the mobile application on your phone, download the
accompanying "MultibaggerBot.apk" from the GitHub Releases page:
https://github.com/MechanicalEngineer007/MultibaggerBot/releases
"""
with open(os.path.join(desktop_bundle_dir, "README.txt"), "w", encoding="utf-8") as f:
    f.write(readme_txt)

# 3. Create ZIP archive
zip_path = os.path.join(release_dir, "MultibaggerBot-Desktop-v1.0.0.zip")
with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zipf:
    for root, dirs, files in os.walk(desktop_bundle_dir):
        for file in files:
            file_path = os.path.join(root, file)
            arcname = os.path.relpath(file_path, desktop_bundle_dir)
            zipf.write(file_path, arcname)

print(f"Created Desktop ZIP: {zip_path} ({os.path.getsize(zip_path)} bytes)")
