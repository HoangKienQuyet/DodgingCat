@echo off
cd /d "%~dp0"
"%~dp0runtime\bin\java.exe" --enable-native-access=javafx.graphics --module-path "%~dp0lib" --add-modules javafx.controls,javafx.swing -cp "%~dp0dodging-cat.jar;%~dp0lib\gson-2.13.2.jar" com.dodgingcat.Launcher %*
set "GAME_EXIT=%ERRORLEVEL%"
if not "%GAME_EXIT%"=="0" pause
exit /b %GAME_EXIT%
