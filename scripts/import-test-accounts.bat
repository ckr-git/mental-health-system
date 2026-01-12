@echo off
chcp 65001 >nul
echo ==========================================
echo 导入测试账号
echo ==========================================
echo.

set /p password=请输入MySQL root密码: 

echo.
echo 正在导入测试账号...
echo.

mysql -u root -p%password% mental_health < create-test-accounts.sql

if %errorlevel% equ 0 (
    echo.
    echo ==========================================
    echo 测试账号导入成功！
    echo ==========================================
    echo.
    echo 可用的测试账号：
    echo.
    echo 【患者】 patient001 / 123456
    echo 【医生】 doctor001 / 123456
    echo 【管理】 admin / 123456
    echo.
    echo ==========================================
) else (
    echo.
    echo 导入失败，请检查：
    echo 1. MySQL服务是否启动
    echo 2. 密码是否正确
    echo 3. mental_health数据库是否存在
    echo.
)

pause
