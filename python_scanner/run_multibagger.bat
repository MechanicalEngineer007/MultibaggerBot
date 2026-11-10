@echo off
set PYTHON_EXE=C:\Users\#Rajan Appa Gawade\.gemini\antigravity\scratch\swing_bot\.venv\Scripts\python.exe

if not exist "%PYTHON_EXE%" (
    python main.py %*
) else (
    "%PYTHON_EXE%" main.py %*
)
