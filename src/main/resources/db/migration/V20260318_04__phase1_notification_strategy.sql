-- ============================================================
-- Phase 1 Upgrade Slice 4: 通知策略中心
-- 偏好矩阵、通知模板、递送状态、升级链
-- ============================================================

-- 1) 通知偏好
CREATE TABLE notification_preference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    category VARCHAR(32) NOT NULL COMMENT 'CRISIS,APPOINTMENT,ASSESSMENT,TREATMENT,SYSTEM,CHAT',
    channel_code VARCHAR(32) NOT NULL COMMENT 'IN_APP,EMAIL,SMS,WEBSOCKET',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    quiet_start TIME NULL COMMENT '静默开始时间',
    quiet_end TIME NULL COMMENT '静默结束时间',
    min_priority VARCHAR(16) NOT NULL DEFAULT 'LOW' COMMENT '最低优先级阈值 LOW,NORMAL,HIGH,URGENT',
    coalesce_minutes INT NOT NULL DEFAULT 0 COMMENT '聚合窗口分钟数（0=不聚合）',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_preference_user_category_channel (user_id, category, channel_code),
    INDEX idx_preference_user (user_id),
    CONSTRAINT fk_preference_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知偏好';

-- 2) 通知模板
CREATE TABLE notification_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    template_code VARCHAR(64) NOT NULL COMMENT '模板编码',
    category VARCHAR(32) NOT NULL COMMENT '通知类别',
    default_priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT '默认优先级',
    title_template VARCHAR(200) NOT NULL COMMENT '标题模板（支持{var}占位）',
    content_template VARCHAR(1000) NOT NULL COMMENT '内容模板',
    action_type VARCHAR(32) NULL COMMENT '动作类型',
    action_template JSON NULL COMMENT '动作模板',
    escalation_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用升级',
    escalation_timeout_minutes INT NULL COMMENT '升级超时分钟',
    escalation_target_role VARCHAR(32) NULL COMMENT '升级目标角色',
    must_ack TINYINT NOT NULL DEFAULT 0 COMMENT '是否必须确认',
    ack_deadline_minutes INT NULL COMMENT '确认截止分钟',
    active TINYINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_template_code (template_code),
    INDEX idx_template_category (category, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知模板';

-- 3) 通知递送记录
CREATE TABLE notification_delivery (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    notification_id BIGINT NOT NULL COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    channel_code VARCHAR(32) NOT NULL COMMENT 'IN_APP,EMAIL,SMS,WEBSOCKET',
    delivery_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,SENT,DELIVERED,FAILED,COALESCED',
    sent_at DATETIME NULL COMMENT '发送时间',
    delivered_at DATETIME NULL COMMENT '送达时间',
    failed_at DATETIME NULL COMMENT '失败时间',
    failure_reason VARCHAR(500) NULL COMMENT '失败原因',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    coalesced_key VARCHAR(128) NULL COMMENT '聚合键',
    coalesced_count INT NOT NULL DEFAULT 1 COMMENT '聚合数量',
    escalated_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否已升级',
    escalated_to_notification_id BIGINT NULL COMMENT '升级后的通知ID',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_delivery_notification (notification_id),
    INDEX idx_delivery_user_channel (user_id, channel_code, delivery_status),
    INDEX idx_delivery_pending (delivery_status, sent_at),
    CONSTRAINT fk_delivery_notification FOREIGN KEY (notification_id) REFERENCES user_notification(id),
    CONSTRAINT fk_delivery_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知递送记录';

-- 4) 扩展现有user_notification表
ALTER TABLE user_notification
    ADD COLUMN template_code VARCHAR(64) NULL COMMENT '模板编码' AFTER source_id,
    ADD COLUMN must_ack TINYINT NOT NULL DEFAULT 0 COMMENT '是否必须确认' AFTER template_code,
    ADD COLUMN ack_deadline DATETIME NULL COMMENT '确认截止' AFTER must_ack,
    ADD COLUMN acked_at DATETIME NULL COMMENT '确认时间' AFTER ack_deadline,
    ADD COLUMN delivery_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,SENT,DELIVERED' AFTER acked_at,
    ADD COLUMN coalesced_key VARCHAR(128) NULL COMMENT '聚合键' AFTER delivery_status,
    ADD COLUMN coalesced_count INT NOT NULL DEFAULT 1 COMMENT '聚合计数' AFTER coalesced_key,
    ADD COLUMN escalation_status VARCHAR(32) NULL COMMENT 'NONE,PENDING,ESCALATED' AFTER coalesced_count,
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER escalation_status,
    ADD INDEX idx_notif_ack_deadline (must_ack, ack_deadline, acked_at),
    ADD INDEX idx_notif_escalation (escalation_status, ack_deadline);

-- 5) 种子数据：通知模板
INSERT INTO notification_template (template_code, category, default_priority, title_template, content_template, escalation_enabled, escalation_timeout_minutes, escalation_target_role, must_ack) VALUES
('CRISIS_NEW', 'CRISIS', 'URGENT', '紧急：{patientName}触发危机预警', '患者{patientName}触发{riskLevel}级别危机预警，请立即查看并处理。', 1, 15, 'ADMIN', 1),
('CRISIS_ESCALATED', 'CRISIS', 'URGENT', '危机升级：{patientName}危机案例需要关注', '危机案例#{caseId}已升级，原因：{reason}。请立即介入。', 1, 10, 'ADMIN', 1),
('APPOINTMENT_REMINDER', 'APPOINTMENT', 'NORMAL', '预约提醒：明天{time}有预约', '您与{doctorName}医生的预约将于{time}进行，请准时到达。', 0, NULL, NULL, 0),
('WAITLIST_OFFER', 'APPOINTMENT', 'HIGH', '候补通知：有空余时段', '{doctorName}医生在{date} {time}有空余时段，请在{deadline}前确认。', 0, NULL, NULL, 0),
('ASSESSMENT_ASSIGNED', 'ASSESSMENT', 'NORMAL', '新评估任务：{scaleName}', '您有一份新的{scaleName}评估需要完成，截止时间：{dueAt}。', 0, NULL, NULL, 0),
('ASSESSMENT_OVERDUE', 'ASSESSMENT', 'HIGH', '评估逾期：{scaleName}', '您的{scaleName}评估已逾期，请尽快完成。', 1, 60, 'DOCTOR', 0),
('ASSESSMENT_RESULT_ALERT', 'ASSESSMENT', 'HIGH', '评估结果需复核：{patientName}', '患者{patientName}的{scaleName}评估结果为{severityLevel}，请复核。', 0, NULL, NULL, 1);
