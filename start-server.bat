@echo off
rem ============================================================
rem  AscentSys - start mock server only
rem  Keep this window open while using the client.
rem ============================================================
chcp 65001 >nul
cd /d "%~dp0"

if not exist "bin\com\ascent\ui\Ascentsys.class" goto NO_BUILD

echo MockServer will listen on 127.0.0.1:5150
echo Close this window (or press Ctrl+C) to stop.
echo.
java -Dfile.encoding=UTF-8 -cp bin com.ascent.server.mock.MockServer
echo.
echo Server stopped.
pause
exit /b 0

:NO_BUILD
echo.
echo [ERROR] bin\ not found. Compile first (see README).
echo.
pause
exit /b 1
