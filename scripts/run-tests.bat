@echo off
chcp 65001 >nul
echo ==========================================
echo    智能心理健康管理系统 - 一键测试
echo ==========================================
echo.

cd /d "%~dp0.."

echo [1/3] 运行后端单元测试...
call mvn test -q
if %errorlevel% neq 0 (
    echo [失败] 后端单元测试未通过
) else (
    echo [成功] 后端单元测试通过
)

echo.
echo [2/3] 运行API自动化测试...
powershell -ExecutionPolicy Bypass -File "%~dp0test-api-auto.ps1"

echo.
echo [3/3] 测试完成
echo ==========================================
pause
