@echo off
setlocal DisableDelayedExpansion
cd /d "%~dp0"
call "%~dp0Prepare.cmd"
if errorlevel 1 (
  pause
  exit /b 1
)
start "" "%~dp0DodgingCat\source"
