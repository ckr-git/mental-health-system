@echo off
chcp 65001 >nul
echo ============================================
echo 登录问题快速修复
echo ============================================
echo.

echo [1/4] 检查端口占用情况...
echo.
echo 后端端口 8080:
netstat -ano | findstr :8080 | findstr LISTENING
if %errorlevel% equ 0 (
    echo ✓ 后端运行中
) else (
    echo ✗ 后端未运行，请先启动后端！
    pause
    exit
)

echo.
echo 前端端口 3000:
netstat -ano | findstr :3000 | findstr LISTENING
if %errorlevel% equ 0 (
    echo ✓ 前端运行中（端口3000）
    echo.
    echo ============================================
    echo 访问地址
    echo ============================================
    echo.
    echo 请在浏览器中访问：
    echo http://localhost:3000
    echo.
    echo 测试账号：
    echo 用户名：patient001
    echo 密码：123456
    echo.
) else (
    echo ✗ 前端未运行在3000端口
    echo.
    echo 请重新启动前端服务：
    echo cd frontend
    echo npm run dev
    echo.
)

echo.
echo [2/4] 检查数据库账号...
mysql -u root -p'root123456' mental_health -e "SELECT username, LEFT(password,10) as pwd, role FROM user WHERE username='patient001'" 2>nul
if %errorlevel% equ 0 (
    echo ✓ 数据库账号存在
) else (
    echo ✗ 无法连接数据库
)

echo.
echo [3/4] 测试登录接口...
powershell -Command "$body='{\"username\":\"patient001\",\"password\":\"123456\"}'; try { $r=Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $body -ContentType 'application/json'; Write-Host '✓ 登录接口正常' -ForegroundColor Green; } catch { Write-Host '✗ 登录接口返回: '$_.Exception.Response.StatusCode -ForegroundColor Red; }"

echo.
echo [4/4] 诊断总结
echo ============================================
echo.
echo 如果所有检查都通过✓，但仍无法登录：
echo.
echo 1. 确认访问的是 http://localhost:3000 而不是 5173
echo 2. 清除浏览器缓存或使用无痕模式
echo 3. 查看浏览器F12开发者工具的Console和Network标签
echo 4. 查看后端控制台是否有错误日志
echo.

pause
