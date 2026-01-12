@echo off
chcp 65001 >nul
echo ============================================
echo 快速测试新接口
echo ============================================
echo.

echo [1] 测试登录接口...
curl -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"patient001\",\"password\":\"123456\"}"
echo.
echo.

echo [2] 获取天气配置...
curl -X GET http://localhost:8080/api/patient/theme/weather ^
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
echo.
echo.

echo 测试完成！
echo 如果看到JSON响应，说明后端正常运行。
pause
