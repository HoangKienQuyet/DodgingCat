@echo off
setlocal DisableDelayedExpansion
cd /d "%~dp0"
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0support\Prepare.ps1"
if errorlevel 1 goto failed
set /p PORTABLE_ROOT=<"%~dp0support\portable-root.txt"
call "%PORTABLE_ROOT%\support\Compile.cmd"
if errorlevel 1 goto failed
if /i not "%PORTABLE_ROOT%\"=="%~dp0" copy /b /y "%PORTABLE_ROOT%\support\app\game-code.jar" "%~dp0support\app\game-code.jar" >nul
if errorlevel 1 goto failed
echo Build succeeded. Run Run.cmd to play the edited source.
pause
exit /b 0
:failed
echo Build failed. Close the game first and check the error above.
pause
exit /b 1
