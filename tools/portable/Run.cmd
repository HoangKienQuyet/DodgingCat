@echo off
setlocal DisableDelayedExpansion
cd /d "%~dp0"
call "%~dp0Prepare.cmd"
if errorlevel 1 (
  echo Preparation failed. See the message above and prepare-assets.log.
  pause
  exit /b 1
)
set /p PORTABLE_ROOT=<"%~dp0portable-root.txt"
start "" "%PORTABLE_ROOT%\runtime\bin\javaw.exe" --enable-native-access=javafx.graphics,javafx.media --module-path "%PORTABLE_ROOT%\app\fx" --add-modules javafx.graphics,javafx.media,javafx.swing -Dfile.encoding=UTF-8 "-Ddodgingcat.profilePathFile=%PORTABLE_ROOT%\profile-location.txt" -cp "%PORTABLE_ROOT%\app\game-code.jar;%PORTABLE_ROOT%\app\gson.jar;%PORTABLE_ROOT%\app\jlayer.jar;%PORTABLE_ROOT%\source\src\main\resources" com.dodgingcat.Launcher
