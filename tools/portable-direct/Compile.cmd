@echo off
chcp 65001 >nul
setlocal DisableDelayedExpansion
cd /d "%~dp0"
set "BUILD_DIR=%~dp0build\%RANDOM%-%RANDOM%"
mkdir "%BUILD_DIR%\classes"
if errorlevel 1 exit /b 1
(for /r "%~dp0..\src\main\java" %%F in (*.java) do @echo "%%F") > "%BUILD_DIR%\sources.txt"
set "SOURCE_LIST=%BUILD_DIR%\sources.txt"
rem javac argument files treat backslashes as escapes, including inside quotes.
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -Command "[IO.File]::WriteAllText($env:SOURCE_LIST,[IO.File]::ReadAllText($env:SOURCE_LIST).Replace('\','/'),[Text.UTF8Encoding]::new($false))"
if errorlevel 1 exit /b 1
"%~dp0runtime\bin\javac.exe" -encoding UTF-8 --release 25 --module-path "%~dp0app\fx" --add-modules javafx.graphics,javafx.media,javafx.swing -cp "%~dp0app\gson.jar;%~dp0app\jlayer.jar" -d "%BUILD_DIR%\classes" @"%BUILD_DIR%\sources.txt"
if errorlevel 1 exit /b 1
"%~dp0runtime\bin\jar.exe" --create --file "%BUILD_DIR%\game-code.jar" -C "%BUILD_DIR%\classes" .
if errorlevel 1 exit /b 1
copy /b "%~dp0app\game-code.jar" "%BUILD_DIR%\previous-game-code.jar" >nul
copy /b /y "%BUILD_DIR%\game-code.jar" "%~dp0app\game-code.jar" >nul
exit /b %errorlevel%
