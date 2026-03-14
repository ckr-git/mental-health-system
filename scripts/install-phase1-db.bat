@echo off
chcp 65001 >nul
echo ============================================
echo 安装第一阶段数据库
echo ============================================
echo.

mysql -u root -proot123456 mental_health < phase1-database.sql

if %errorlevel% equ 0 (
    echo.
    echo ✓ 数据库创建成功！
    echo.
    echo 验证表结构：
    mysql -u root -proot123456 mental_health -e "SHOW TABLES LIKE 'mood_%%'; SHOW TABLES LIKE 'time_%%'; SHOW TABLES LIKE 'user_theme_%%'; SHOW TABLES LIKE 'weather_%%';"
    echo.
    echo 查看天气配置：
    mysql -u root -proot123456 mental_health -e "SELECT * FROM weather_config;"
) else (
    echo.
    echo ✗ 数据库创建失败
)

pause
