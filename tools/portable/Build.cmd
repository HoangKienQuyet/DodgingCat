@echo off
setlocal DisableDelayedExpansion
cd /d "%~dp0"
call "%~dp0Prepare.cmd"
if errorlevel 1 goto failed
set /p PORTABLE_ROOT=<"%~dp0portable-root.txt"
call "%PORTABLE_ROOT%\Compile.cmd"
if errorlevel 1 goto failed
if /i not "%PORTABLE_ROOT%"=="%~dp0DodgingCat" copy /b /y "%PORTABLE_ROOT%\app\game-code.jar" "%~dp0DodgingCat\app\game-code.jar" >nul
if errorlevel 1 goto failed
echo Build succeeded. Run Run.cmd to play the newly compiled source.
pause
exit /b 0
:failed
echo Build failed. Close the game first and check the error message above.
pause
exit /b 1
