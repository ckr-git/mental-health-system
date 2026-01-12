@echo off
echo Starting Redis on Windows...

if not exist "C:\Redis\redis-server.exe" (
    echo [ERROR] Redis not installed in C:\Redis
    echo Please download from: https://github.com/tporadowski/redis/releases
    pause
    exit /b 1
)

echo Starting Redis server...
start "Redis Server" "C:\Redis\redis-server.exe" "C:\Redis\redis.windows.conf"

timeout /t 3 >nul

echo.
echo Testing connection...
"C:\Redis\redis-cli.exe" ping

if errorlevel 1 (
    echo [ERROR] Redis failed to start
) else (
    echo.
    echo ========================================
    echo Redis is running!
    echo Server: localhost:6379
    echo Data Dir: C:\RedisData
    echo ========================================
)
pause
