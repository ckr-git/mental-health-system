-- =============================================
-- 测试账号SQL脚本
-- 所有账号的密码都是：123456
-- =============================================

USE mental_health;

-- 1. 患者测试账号
-- 注意：密码哈希中的 $ 符号在某些环境下需要转义
INSERT INTO `user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `gender`, `age`, `role`, `status`, `deleted`) 
VALUES 
('patient001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae', '张三（患者）', NULL, '13800000001', 'patient001@test.com', 1, 25, 'PATIENT', 1, 0),
('patient002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae', '李四（患者）', NULL, '13800000002', 'patient002@test.com', 2, 30, 'PATIENT', 1, 0),
('patient003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae', '王五（患者）', NULL, '13800000003', 'patient003@test.com', 1, 28, 'PATIENT', 1, 0);

-- 2. 医生测试账号
INSERT INTO `user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `gender`, `age`, `role`, `status`, `deleted`) 
VALUES 
('doctor001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae', '赵医生', NULL, '13900000001', 'doctor001@test.com', 2, 40, 'DOCTOR', 1, 0),
('doctor002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae', '钱医生', NULL, '13900000002', 'doctor002@test.com', 1, 38, 'DOCTOR', 1, 0);

-- 3. 管理员测试账号
INSERT INTO `user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `gender`, `age`, `role`, `status`, `deleted`) 
VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae', '系统管理员', NULL, '13700000001', 'admin@test.com', 1, 35, 'ADMIN', 1, 0);

-- 4. 为医生添加详细信息
INSERT INTO `doctor_info` (`user_id`, `title`, `department`, `hospital`, `specialty`, `introduction`, `experience_years`, `rating`, `consultation_count`) 
SELECT 
    u.id,
    '主任医师',
    '心理科',
    '中国人民医院',
    '焦虑症、抑郁症、睡眠障碍的诊断与治疗',
    '从事心理健康工作15年，擅长认知行为疗法（CBT）和正念疗法，帮助数千名患者走出心理困境。',
    15,
    4.8,
    1250
FROM `user` u WHERE u.username = 'doctor001';

INSERT INTO `doctor_info` (`user_id`, `title`, `department`, `hospital`, `specialty`, `introduction`, `experience_years`, `rating`, `consultation_count`) 
SELECT 
    u.id,
    '副主任医师',
    '精神科',
    '北京协和医院',
    '青少年心理问题、家庭关系咨询、情绪管理',
    '心理学博士，专注青少年心理健康研究10年，擅长家庭治疗和情绪焦点疗法。',
    10,
    4.9,
    890
FROM `user` u WHERE u.username = 'doctor002';

-- 5. 为患者添加一些示例症状记录
INSERT INTO `symptom_record` (`user_id`, `symptom_type`, `description`, `severity`, `mood_score`, `energy_level`, `sleep_quality`, `stress_level`, `tags`, `deleted`) 
SELECT 
    u.id,
    '焦虑',
    '今天工作压力很大，感觉心跳加快，手心出汗，难以集中注意力。',
    2,
    4,
    5,
    6,
    8,
    '["工作压力", "焦虑"]',
    0
FROM `user` u WHERE u.username = 'patient001';

INSERT INTO `symptom_record` (`user_id`, `symptom_type`, `description`, `severity`, `mood_score`, `energy_level`, `sleep_quality`, `stress_level`, `tags`, `deleted`, `create_time`) 
SELECT 
    u.id,
    '失眠',
    '昨晚失眠到凌晨3点才睡着，早上起来很累。',
    2,
    5,
    3,
    3,
    7,
    '["失眠", "疲劳"]',
    0,
    DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM `user` u WHERE u.username = 'patient001';

INSERT INTO `symptom_record` (`user_id`, `symptom_type`, `description`, `severity`, `mood_score`, `energy_level`, `sleep_quality`, `stress_level`, `tags`, `deleted`, `create_time`) 
SELECT 
    u.id,
    '抑郁',
    '今天心情很低落，对什么都提不起兴趣，感觉很疲惫。',
    3,
    3,
    2,
    4,
    6,
    '["抑郁", "疲惫"]',
    0,
    DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM `user` u WHERE u.username = 'patient001';

SELECT '测试账号创建完成！' as message;
SELECT '所有账号密码都是：123456' as password_info;
