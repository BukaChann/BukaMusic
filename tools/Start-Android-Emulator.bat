@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-emulator.ps1"
if errorlevel 1 (
  echo.
  echo Failed to start the Android emulator.
  pause
)
endlocal
