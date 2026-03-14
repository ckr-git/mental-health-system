-- =============================================
-- 确保医生数据存在
-- 执行此脚本以修复医生列表为空的问题
-- =============================================

USE mental_health;

-- 检查并插入医生账号（如果不存在）
INSERT IGNORE INTO `user` (`username`, `password`, `nickname`, `phone`, `email`, `gender`, `age`, `role`, `status`, `deleted`)
VALUES
('doctor001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae', '赵医生', '13900000001', 'doctor001@test.com', 2, 40, 'DOCTOR', 1, 0),
('doctor002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae', '钱医生', '13900000002', 'doctor002@test.com', 1, 38, 'DOCTOR', 1, 0);

-- 确保医生状态为启用
UPDATE `user` SET `status` = 1 WHERE `role` = 'DOCTOR' AND `deleted` = 0;

-- 检查并插入医生详细信息
INSERT IGNORE INTO `doctor_info` (`user_id`, `title`, `department`, `hospital`, `specialty`, `introduction`, `experience_years`, `rating`, `consultation_count`)
SELECT u.id, '主任医师', '心理科', '中国人民医院', '焦虑症、抑郁症、睡眠障碍', '从事心理健康工作15年', 15, 4.8, 1250
FROM `user` u WHERE u.username = 'doctor001' AND NOT EXISTS (SELECT 1 FROM `doctor_info` WHERE user_id = u.id);

INSERT IGNORE INTO `doctor_info` (`user_id`, `title`, `department`, `hospital`, `specialty`, `introduction`, `experience_years`, `rating`, `consultation_count`)
SELECT u.id, '副主任医师', '精神科', '北京协和医院', '青少年心理问题、家庭关系咨询', '心理学博士，专注青少年心理健康研究10年', 10, 4.9, 890
FROM `user` u WHERE u.username = 'doctor002' AND NOT EXISTS (SELECT 1 FROM `doctor_info` WHERE user_id = u.id);

-- 验证结果
SELECT '医生数据检查完成' as message;
SELECT id, username, nickname, role, status FROM `user` WHERE role = 'DOCTOR';
