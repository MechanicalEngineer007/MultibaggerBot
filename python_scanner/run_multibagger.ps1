$venvPython = "C:\Users\#Rajan Appa Gawade\.gemini\antigravity\scratch\swing_bot\.venv\Scripts\python.exe"

if (Test-Path $venvPython) {
    & $venvPython main.py @args
} else {
    python main.py @args
}
