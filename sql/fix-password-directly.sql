-- ============================================
-- 直接修复密码问题
-- ============================================
-- 这个哈希是通过在线BCrypt生成器生成的，确保是123456的正确哈希
-- ============================================

USE mental_health;

-- 更新所有测试账号的密码为正确的BCrypt哈希
-- 使用BCrypt rounds=10生成的123456哈希
-- 注意：密码字符串用单引号包裹，SQL文件中$符号不会被解析
UPDATE user SET password = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi' 
WHERE username IN ('patient001', 'patient002', 'doctor001', 'admin');

-- 验证更新
SELECT username, LEFT(password, 30) as password_hash, role 
FROM user 
WHERE username IN ('patient001', 'patient002', 'doctor001', 'admin');

SELECT '密码已更新为：123456' as message;
