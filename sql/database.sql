-- 智能AI心理健康管理系统数据库

CREATE DATABASE IF NOT EXISTS mental_health DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mental_health;

-- 用户表（患者、医生、管理员）
CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（加密）',
  `nickname` VARCHAR(50) COMMENT '昵称',
  `avatar` VARCHAR(255) COMMENT '头像',
  `phone` VARCHAR(20) COMMENT '手机号',
  `email` VARCHAR(100) COMMENT '邮箱',
  `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
  `age` INT COMMENT '年龄',
  `role` VARCHAR(20) DEFAULT 'PATIENT' COMMENT '角色：PATIENT-患者 DOCTOR-医生 ADMIN-管理员',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_username (`username`),
  INDEX idx_role (`role`)
) ENGINE=InnoDB COMMENT='用户表';

-- 医生信息表
CREATE TABLE `doctor_info` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(50) COMMENT '职称',
  `department` VARCHAR(100) COMMENT '科室',
  `hospital` VARCHAR(200) COMMENT '医院',
  `specialty` VARCHAR(500) COMMENT '专长',
  `introduction` TEXT COMMENT '个人简介',
  `experience_years` INT COMMENT '从业年限',
  `rating` DECIMAL(3,2) DEFAULT 5.00 COMMENT '评分',
  `consultation_count` INT DEFAULT 0 COMMENT '咨询次数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB COMMENT='医生信息表';

-- 症状记录表
CREATE TABLE `symptom_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '患者ID',
  `symptom_type` VARCHAR(50) COMMENT '症状类型',
  `description` TEXT COMMENT '症状描述',
  `severity` TINYINT COMMENT '严重程度：1-轻度 2-中度 3-重度',
  `mood_score` INT COMMENT '情绪评分（1-10）',
  `energy_level` INT COMMENT '活力水平（1-10）',
  `sleep_quality` INT COMMENT '睡眠质量（1-10）',
  `stress_level` INT COMMENT '压力水平（1-10）',
  `tags` VARCHAR(500) COMMENT '标签（JSON数组）',
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user (`user_id`),
  INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB COMMENT='症状记录表';

-- 心理测评表
CREATE TABLE `psychological_test` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '测评名称',
  `type` VARCHAR(50) COMMENT '测评类型',
  `description` TEXT COMMENT '测评说明',
  `questions` TEXT COMMENT '题目（JSON）',
  `scoring_rules` TEXT COMMENT '评分规则（JSON）',
  `duration` INT DEFAULT 30 COMMENT '预计时长（分钟）',
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='心理测评表';

-- 测评结果表
CREATE TABLE `test_result` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `test_id` BIGINT NOT NULL,
  `answers` TEXT COMMENT '答案（JSON）',
  `score` INT COMMENT '得分',
  `result_level` VARCHAR(20) COMMENT '结果等级',
  `result_desc` TEXT COMMENT '结果描述',
  `suggestions` TEXT COMMENT '建议',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (`user_id`),
  INDEX idx_test (`test_id`),
  FOREIGN KEY (`test_id`) REFERENCES `psychological_test`(`id`)
) ENGINE=InnoDB COMMENT='测评结果表';

-- 评估报告表
CREATE TABLE `assessment_report` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '患者ID',
  `doctor_id` BIGINT COMMENT '医生ID',
  `report_type` VARCHAR(50) COMMENT '报告类型',
  `title` VARCHAR(200) COMMENT '报告标题',
  `content` TEXT COMMENT '报告内容',
  `diagnosis` TEXT COMMENT '诊断结果',
  `suggestions` TEXT COMMENT '治疗建议',
  `is_ai_generated` TINYINT DEFAULT 0 COMMENT '是否AI生成',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user (`user_id`),
  INDEX idx_doctor (`doctor_id`)
) ENGINE=InnoDB COMMENT='评估报告表';

-- 心理资源表
CREATE TABLE `mental_resource` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL COMMENT '资源标题',
  `type` VARCHAR(50) COMMENT '资源类型：文章、视频、音频、课程',
  `category` VARCHAR(50) COMMENT '分类',
  `content` TEXT COMMENT '内容',
  `file_url` VARCHAR(500) COMMENT '文件地址',
  `cover_image` VARCHAR(500) COMMENT '封面图',
  `author` VARCHAR(100) COMMENT '作者',
  `duration` INT COMMENT '时长（分钟）',
  `tags` VARCHAR(500) COMMENT '标签',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `download_count` INT DEFAULT 0 COMMENT '下载次数',
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_type (`type`),
  INDEX idx_category (`category`)
) ENGINE=InnoDB COMMENT='心理资源表';

-- 用户资源浏览记录表
CREATE TABLE `resource_view_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `resource_id` BIGINT NOT NULL,
  `view_duration` INT COMMENT '浏览时长（秒）',
  `rating` TINYINT COMMENT '评分（1-5）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_resource (`user_id`, `resource_id`)
) ENGINE=InnoDB COMMENT='资源浏览记录表';

-- 医患沟通记录表
CREATE TABLE `consultation_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `patient_id` BIGINT NOT NULL COMMENT '患者ID',
  `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
  `type` VARCHAR(20) COMMENT '类型：文字、语音、视频',
  `status` TINYINT COMMENT '状态：0-待处理 1-进行中 2-已完成',
  `start_time` DATETIME COMMENT '开始时间',
  `end_time` DATETIME COMMENT '结束时间',
  `duration` INT COMMENT '时长（分钟）',
  `summary` TEXT COMMENT '咨询摘要',
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_patient (`patient_id`),
  INDEX idx_doctor (`doctor_id`)
) ENGINE=InnoDB COMMENT='医患沟通记录表';

-- 消息记录表
CREATE TABLE `message` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `consultation_id` BIGINT COMMENT '咨询记录ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送人ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收人ID',
  `content` TEXT COMMENT '消息内容',
  `message_type` VARCHAR(20) DEFAULT 'text' COMMENT '消息类型：text、image、voice、video',
  `file_url` VARCHAR(500) COMMENT '文件地址',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_consultation (`consultation_id`),
  INDEX idx_sender_receiver (`sender_id`, `receiver_id`)
) ENGINE=InnoDB COMMENT='消息记录表';

-- AI对话记录表
CREATE TABLE `ai_conversation` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `question` TEXT COMMENT '用户问题',
  `answer` TEXT COMMENT 'AI回答',
  `context` TEXT COMMENT '上下文（JSON）',
  `feedback` TINYINT COMMENT '反馈：1-有帮助 2-无帮助',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (`user_id`)
) ENGINE=InnoDB COMMENT='AI对话记录表';

-- 预约表
CREATE TABLE `appointment` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `patient_id` BIGINT NOT NULL,
  `doctor_id` BIGINT NOT NULL,
  `appointment_time` DATETIME NOT NULL COMMENT '预约时间',
  `type` VARCHAR(20) COMMENT '预约类型',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-待确认 1-已确认 2-已完成 3-已取消',
  `reason` TEXT COMMENT '预约原因',
  `remark` VARCHAR(500) COMMENT '备注',
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_patient (`patient_id`),
  INDEX idx_doctor (`doctor_id`),
  INDEX idx_appointment_time (`appointment_time`)
) ENGINE=InnoDB COMMENT='预约表';

-- 系统通知表
CREATE TABLE `notification` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
  `type` VARCHAR(50) COMMENT '通知类型',
  `title` VARCHAR(200) COMMENT '标题',
  `content` TEXT COMMENT '内容',
  `is_read` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (`user_id`)
) ENGINE=InnoDB COMMENT='系统通知表';

-- 用户情绪变化记录表
CREATE TABLE `emotion_change_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `mood_score` INT COMMENT '情绪评分',
  `anxiety_level` INT COMMENT '焦虑程度',
  `depression_level` INT COMMENT '抑郁程度',
  `stress_level` INT COMMENT '压力水平',
  `energy_level` INT COMMENT '活力水平',
  `sleep_quality` INT COMMENT '睡眠质量',
  `notes` TEXT COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_date (`user_id`, `record_date`),
  INDEX idx_user (`user_id`)
) ENGINE=InnoDB COMMENT='情绪变化记录表';

-- 数据统计表
CREATE TABLE `statistics` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `total_users` INT DEFAULT 0 COMMENT '总用户数',
  `new_users` INT DEFAULT 0 COMMENT '新增用户',
  `active_users` INT DEFAULT 0 COMMENT '活跃用户',
  `total_consultations` INT DEFAULT 0 COMMENT '总咨询数',
  `completed_consultations` INT DEFAULT 0 COMMENT '完成咨询数',
  `total_tests` INT DEFAULT 0 COMMENT '总测评数',
  `resource_views` INT DEFAULT 0 COMMENT '资源浏览量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_stat_date (`stat_date`)
) ENGINE=InnoDB COMMENT='数据统计表';

-- 系统日志表
CREATE TABLE `system_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT,
  `username` VARCHAR(50),
  `operation` VARCHAR(100) COMMENT '操作',
  `method` VARCHAR(200) COMMENT '方法名',
  `params` TEXT COMMENT '参数',
  `ip` VARCHAR(50),
  `time` BIGINT COMMENT '耗时（ms）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (`user_id`),
  INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB COMMENT='系统日志表';
