@echo off
chcp 65001 >nul
echo ============================================
echo 启动后端服务
echo ============================================
echo.

cd /d "E:\ddd\智能心里健康管理系统"

echo 正在启动 Spring Boot...
echo 请耐心等待，首次启动可能需要1-2分钟
echo.

mvn spring-boot:run

pause
