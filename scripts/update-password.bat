@echo off
chcp 65001 >nul
echo ============================================
echo 更新测试账号密码
echo ============================================
echo.

set /p dbpass=请输入MySQL root密码: 

echo.
echo 正在更新密码...
echo.

mysql -u root -p%dbpass% mental_health -e "UPDATE user SET password = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi' WHERE username IN ('patient001', 'patient002', 'doctor001', 'admin');"

if %errorlevel% equ 0 (
    echo.
    echo ✓ 密码更新成功！
    echo.
    echo 验证更新结果：
    mysql -u root -p%dbpass% mental_health -e "SELECT username, LENGTH(password) as pwd_len, LEFT(password,10) as pwd_start FROM user WHERE username='patient001';"
    echo.
    echo 如果 pwd_len = 60 且 pwd_start = $2a$10$92I，则密码正确！
    echo.
    echo 现在可以使用以下账号登录：
    echo 用户名：patient001
    echo 密码：123456
    echo.
) else (
    echo.
    echo ✗ 密码更新失败
    echo.
)

pause
