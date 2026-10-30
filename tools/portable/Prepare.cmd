@echo off
setlocal DisableDelayedExpansion
cd /d "%~dp0"
set "EXPECTED=@PAYLOAD_SHA256@"
set "CURRENT="
if exist "%~dp0.payload-ready" set /p CURRENT=<"%~dp0.payload-ready"
if "%CURRENT%"=="%EXPECTED%" if exist "%~dp0DodgingCat\runtime\bin\java.exe" if exist "%~dp0DodgingCat\DodgingCat.exe" goto assets
echo Preparing bundled Java and game files. No installation or download is required.
"%~dp07zr.exe" x "%~dp0game.7z" -o"%~dp0." -y -bso0
if errorlevel 1 exit /b 1
>"%~dp0.payload-ready" echo %EXPECTED%
:assets
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0Resolve-root.ps1"
if errorlevel 1 exit /b 1
set /p PORTABLE_ROOT=<"%~dp0portable-root.txt"
if /i "%~1"=="repair" if exist "%PORTABLE_ROOT%\.images-ready" del /q "%PORTABLE_ROOT%\.images-ready"
if /i "%~1"=="repair" if exist "%PORTABLE_ROOT%\.audio-ready" del /q "%PORTABLE_ROOT%\.audio-ready"
"%PORTABLE_ROOT%\runtime\bin\java.exe" -Xmx512m -Djava.awt.headless=true -cp "%PORTABLE_ROOT%\app\game-code.jar" com.dodgingcat.app.PortableAssets "%PORTABLE_ROOT%"
if errorlevel 1 exit /b 1
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0Sync-assets.ps1"
exit /b %errorlevel%
