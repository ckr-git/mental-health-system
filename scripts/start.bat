@echo off
echo ========================================
echo Mental Health System - Start All
echo ========================================
echo.

REM Check if first run
if not exist "frontend\node_modules\" (
    echo [TIP] First run detected. Installing frontend dependencies...
    cd frontend
    call npm install
    cd ..
    echo.
)

REM Start Redis
echo [1/4] Starting Redis...
if exist "C:\Redis\redis-server.exe" (
    start "Redis Server" "C:\Redis\redis-server.exe" "C:\Redis\redis.windows.conf"
    timeout /t 2 >nul
    echo Redis started
) else (
    echo [WARNING] Redis not found in C:\Redis
    echo Run start-redis-windows.bat to start Redis manually
)

REM Check MySQL
echo.
echo [2/4] Checking MySQL...
sc query MySQL80 | findstr "RUNNING" >nul
if errorlevel 1 (
    echo Starting MySQL...
    net start MySQL80 >nul 2>&1
)
echo MySQL is running

REM Start Backend
echo.
echo [3/4] Starting Backend...
start "Mental Health Backend" cmd /c "mvn spring-boot:run && pause"
echo Backend is starting (takes ~30 seconds)...
timeout /t 15 >nul

REM Start Frontend
echo.
echo [4/4] Starting Frontend...
cd frontend
start "Mental Health Frontend" cmd /c "npm run dev && pause"
cd ..

echo.
echo ========================================
echo All Services Started!
echo ========================================
echo Frontend: http://localhost:3000
echo Backend:  http://localhost:8080
echo.
echo Run stop.bat to shutdown all services
echo ========================================
pause
