@echo off
setlocal DisableDelayedExpansion
cd /d "%~dp0"
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0support\Prepare.ps1"
if errorlevel 1 (
  echo Preparation failed. Extract the entire ZIP to a writable folder.
  pause
  exit /b 1
)
set /p PORTABLE_ROOT=<"%~dp0support\portable-root.txt"
start "" "%PORTABLE_ROOT%\support\runtime\bin\javaw.exe" --enable-native-access=javafx.graphics,javafx.media --module-path "%PORTABLE_ROOT%\support\app\fx" --add-modules javafx.graphics,javafx.media,javafx.swing -Dfile.encoding=UTF-8 "-Ddodgingcat.profilePathFile=%PORTABLE_ROOT%\support\profile-location.txt" -cp "%PORTABLE_ROOT%\support\app\game-code.jar;%PORTABLE_ROOT%\support\app\gson.jar;%PORTABLE_ROOT%\support\app\jlayer.jar;%PORTABLE_ROOT%\src\main\resources" com.dodgingcat.Launcher
