@echo off
echo ========================================
echo Stop All Services
echo ========================================
echo.

echo Stopping Frontend (port 3000)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3000 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo Stopping Backend (port 8080)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo Stopping Redis (port 6379)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :6379 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo Closing command windows...
taskkill /F /FI "WINDOWTITLE eq Mental Health*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq Redis*" >nul 2>&1

echo.
echo All services stopped!
pause
