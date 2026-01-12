-- 修复测试账号
-- 密码: 123456 对应的BCrypt哈希

USE mental_health;

-- 更新admin密码为123456
UPDATE `user` SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae' WHERE username = 'admin';

-- 插入患者测试账号（如果不存在）
INSERT IGNORE INTO `user` (`username`, `password`, `nickname`, `phone`, `role`, `status`, `deleted`)
VALUES
('patient001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae', '张三', '13800000001', 'PATIENT', 1, 0),
('patient002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae', '李四', '13800000002', 'PATIENT', 1, 0);

-- 插入医生测试账号（如果不存在）
INSERT IGNORE INTO `user` (`username`, `password`, `nickname`, `phone`, `role`, `status`, `deleted`)
VALUES
('doctor001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae', '赵医生', '13900000001', 'DOCTOR', 1, 0),
('doctor002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae', '钱医生', '13900000002', 'DOCTOR', 1, 0);

SELECT username, nickname, role, status FROM user WHERE username IN ('admin', 'patient001', 'patient002', 'doctor001', 'doctor002');
