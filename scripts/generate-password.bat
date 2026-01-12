@echo off
chcp 65001 >nul
echo 正在生成BCrypt密码哈希...
echo.

cd "E:\ddd\智能心里健康管理系统"

java -cp "target/classes;%USERPROFILE%\.m2\repository\org\springframework\security\spring-security-crypto\6.2.0\spring-security-crypto-6.2.0.jar;%USERPROFILE%\.m2\repository\org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar" -Dfile.encoding=UTF-8 com.mental.health.PasswordTest

pause
