@echo off
rem ============================================================
rem  AscentSys - start everything (mock server + client)
rem  Double click this file.
rem ============================================================
chcp 65001 >nul
cd /d "%~dp0"

if not exist "bin\com\ascent\ui\Ascentsys.class" goto NO_BUILD
where javaw >nul 2>nul
if errorlevel 1 goto NO_JAVA

echo Starting mock server on port 5150 ...
start "AscentSys Server (close this window to stop)" cmd /k "java -Dfile.encoding=UTF-8 -cp bin com.ascent.server.mock.MockServer"

rem Use ping instead of timeout: timeout fails when input is redirected
ping -n 4 127.0.0.1 >nul

echo Starting client ...
start "" javaw -Dfile.encoding=UTF-8 -cp bin com.ascent.ui.Ascentsys
exit /b 0

:NO_BUILD
echo.
echo [ERROR] bin\ not found or not up to date.
echo Compile first (see README):
echo   javac -encoding UTF-8 --release 11 -d bin ^<all .java files^>
echo   copy src\images\* bin\images
echo.
pause
exit /b 1

:NO_JAVA
echo.
echo [ERROR] javaw not found. Install JDK 11+ and add its bin folder to PATH.
echo.
pause
exit /b 1
