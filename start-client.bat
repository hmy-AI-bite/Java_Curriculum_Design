@echo off
rem ============================================================
rem  AscentSys - start client only
rem  The mock server must be running first (start-server.bat).
rem ============================================================
chcp 65001 >nul
cd /d "%~dp0"

if not exist "bin\com\ascent\ui\Ascentsys.class" goto NO_BUILD

start "" javaw -Dfile.encoding=UTF-8 -cp bin com.ascent.ui.Ascentsys
exit /b 0

:NO_BUILD
echo.
echo [ERROR] bin\ not found. Compile first (see README).
echo.
pause
exit /b 1
