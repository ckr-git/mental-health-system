@echo off
echo ========================================
echo Database Initialization
echo ========================================
echo.

echo [1/3] Checking MySQL...
where mysql >nul 2>&1
if errorlevel 1 (
    echo [ERROR] MySQL not found
    pause
    exit /b 1
)

echo [2/3] Creating database...
echo Please enter MySQL root password:
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS mental_health DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if errorlevel 1 (
    echo [ERROR] Database creation failed
    pause
    exit /b 1
)

echo [3/3] Importing database schema...
mysql -u root -p mental_health < database.sql
if errorlevel 1 (
    echo [ERROR] Schema import failed
    pause
    exit /b 1
)

echo.
echo ========================================
echo Database initialized successfully!
echo Database name: mental_health
echo ========================================
pause
