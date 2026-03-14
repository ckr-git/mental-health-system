-- =====================================================
-- Phase 1: Platform Foundations Migration
-- 患者临床档案 + 异步事件Outbox + WebSocket在线状态
-- =====================================================

-- 1) 患者临床档案表（归一化 Profile.vue 中已收集但未持久化的字段）
CREATE TABLE IF NOT EXISTS patient_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '关联 user.id (PATIENT)',
  real_name VARCHAR(50) NULL COMMENT '真实姓名',
  birth_date DATE NULL COMMENT '出生日期',
  marital_status VARCHAR(20) NULL COMMENT '婚姻状况',
  occupation VARCHAR(100) NULL COMMENT '职业',
  emergency_contact_name VARCHAR(50) NULL COMMENT '紧急联系人姓名',
  emergency_contact_phone VARCHAR(20) NULL COMMENT '紧急联系人电话',
  emergency_contact_relation VARCHAR(30) NULL COMMENT '紧急联系人关系',
  introduction TEXT NULL COMMENT '个人简介',
  medical_history TEXT NULL COMMENT '既往病史',
  allergy_history TEXT NULL COMMENT '过敏史',
  family_history TEXT NULL COMMENT '家族病史',
  consent_flags JSON NULL COMMENT '知情同意标志 {"emergency_contact":true,"ai_analysis":true}',
  intake_tags JSON NULL COMMENT '入院标签',
  profile_version INT NOT NULL DEFAULT 1 COMMENT '档案版本号',
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_patient_profile_user (user_id),
  CONSTRAINT fk_patient_profile_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者临床档案';

-- 2) 异步事件发布表（Outbox 模式）
CREATE TABLE IF NOT EXISTS outbox_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  aggregate_type VARCHAR(50) NOT NULL COMMENT '聚合类型',
  aggregate_id BIGINT NOT NULL COMMENT '聚合ID',
  event_type VARCHAR(100) NOT NULL COMMENT '事件类型',
  event_key VARCHAR(100) NOT NULL COMMENT '幂等键',
  payload JSON NOT NULL COMMENT '事件负载',
  headers JSON NULL COMMENT '事件头',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,PROCESSING,SENT,FAILED,DEAD',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  max_retry_count INT NOT NULL DEFAULT 12 COMMENT '最大重试次数',
  next_retry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次重试时间',
  last_error VARCHAR(500) NULL COMMENT '最后一次错误',
  created_by BIGINT NULL COMMENT '创建者ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_outbox_event_key (event_key),
  INDEX idx_outbox_dispatch (status, next_retry_time),
  INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步事件发布表(Outbox模式)';

-- 3) WebSocket 在线状态追踪
CREATE TABLE IF NOT EXISTS user_presence (
  user_id BIGINT PRIMARY KEY COMMENT '关联 user.id',
  websocket_session_id VARCHAR(64) NULL COMMENT 'WebSocket会话ID',
  client_type VARCHAR(20) NOT NULL DEFAULT 'WEB' COMMENT '客户端类型',
  online_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' COMMENT '在线状态: ONLINE,OFFLINE,AWAY',
  last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  connect_time DATETIME NULL COMMENT '连接时间',
  disconnect_time DATETIME NULL COMMENT '断开时间',
  CONSTRAINT fk_user_presence_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户在线状态';

-- 4) 回填已有患者的档案记录
INSERT INTO patient_profile (user_id, profile_version)
SELECT id, 1 FROM user
WHERE role = 'PATIENT' AND deleted = 0
  AND id NOT IN (SELECT user_id FROM patient_profile WHERE deleted = 0);
