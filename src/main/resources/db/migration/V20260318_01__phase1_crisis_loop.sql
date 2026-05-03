-- ============================================================
-- Phase 1 Upgrade Slice 1: 危机案例闭环
-- 将分散的risk_event/crisis_alert归并为可追踪的危机案例
-- ============================================================

-- 1) 危机案例（聚合多个risk_event和crisis_alert）
CREATE TABLE crisis_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    owner_doctor_id BIGINT NULL COMMENT '主责医生ID',
    case_status VARCHAR(32) NOT NULL DEFAULT 'NEW' COMMENT 'NEW,TRIAGED,ACKED,CONTACTING,INTERVENING,ESCALATED,STABILIZED,RESOLVED,POST_REVIEW',
    triage_level VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT 'MEDIUM,HIGH,CRITICAL',
    case_source VARCHAR(32) NOT NULL COMMENT 'AUTO_RISK,MANUAL,ASSESSMENT,DIARY,CHAT',
    title VARCHAR(200) NOT NULL COMMENT '案例标题',
    summary TEXT NULL COMMENT '案例摘要',
    initial_risk_event_id BIGINT NULL COMMENT '触发的初始风险事件ID',
    merged_event_count INT NOT NULL DEFAULT 1 COMMENT '归并的事件数',
    reopen_count INT NOT NULL DEFAULT 0 COMMENT '重新打开次数',
    next_action_deadline DATETIME NULL COMMENT '下一步行动截止时间',
    sla_deadline DATETIME NULL COMMENT 'SLA截止时间',
    escalation_chain_json JSON NULL COMMENT '升级链配置',
    opened_at DATETIME NOT NULL COMMENT '开启时间',
    triaged_at DATETIME NULL COMMENT '分诊时间',
    triaged_by BIGINT NULL COMMENT '分诊人',
    resolved_at DATETIME NULL COMMENT '解决时间',
    resolved_by BIGINT NULL COMMENT '解决人',
    resolution_code VARCHAR(32) NULL COMMENT 'STABLE,REFERRED,HOSPITALIZED,FALSE_POSITIVE',
    resolution_note TEXT NULL COMMENT '解决备注',
    post_review_due_at DATETIME NULL COMMENT '复盘截止时间',
    post_review_completed_at DATETIME NULL COMMENT '复盘完成时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_crisis_case_patient_status (patient_id, case_status),
    INDEX idx_crisis_case_owner_status (owner_doctor_id, case_status, next_action_deadline),
    INDEX idx_crisis_case_triage_sla (triage_level, case_status, sla_deadline),
    CONSTRAINT fk_crisis_case_patient FOREIGN KEY (patient_id) REFERENCES user(id),
    CONSTRAINT fk_crisis_case_doctor FOREIGN KEY (owner_doctor_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='危机案例';

-- 2) 危机升级步骤
CREATE TABLE crisis_escalation_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    crisis_case_id BIGINT NOT NULL COMMENT '危机案例ID',
    step_order INT NOT NULL DEFAULT 1 COMMENT '步骤序号',
    escalation_type VARCHAR(32) NOT NULL COMMENT 'TIMEOUT,MANUAL,RULE_TRIGGER,REOPEN',
    from_status VARCHAR(32) NOT NULL COMMENT '原状态',
    to_status VARCHAR(32) NOT NULL COMMENT '目标状态',
    from_doctor_id BIGINT NULL COMMENT '原责任医生',
    to_doctor_id BIGINT NULL COMMENT '新责任医生',
    timeout_minutes INT NULL COMMENT '超时分钟数',
    deadline_at DATETIME NULL COMMENT '截止时间',
    executed_at DATETIME NULL COMMENT '执行时间',
    executed_by BIGINT NULL COMMENT '执行人（NULL表示系统自动）',
    step_result VARCHAR(32) NULL COMMENT 'SUCCESS,TIMEOUT,SKIPPED',
    note VARCHAR(500) NULL COMMENT '备注',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_escalation_case_order (crisis_case_id, step_order),
    INDEX idx_escalation_deadline (step_result, deadline_at),
    CONSTRAINT fk_escalation_case FOREIGN KEY (crisis_case_id) REFERENCES crisis_case(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='危机升级步骤';

-- 3) 患者安全计划
CREATE TABLE patient_safety_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    created_by BIGINT NOT NULL COMMENT '创建人（医生）',
    plan_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'DRAFT,ACTIVE,ARCHIVED',
    warning_signs TEXT NULL COMMENT '警告信号（JSON数组）',
    coping_strategies TEXT NULL COMMENT '应对策略（JSON数组）',
    support_contacts TEXT NULL COMMENT '支持联系人（JSON数组: name,phone,relation）',
    safe_environment TEXT NULL COMMENT '安全环境措施',
    professional_contacts TEXT NULL COMMENT '专业联系人（JSON数组）',
    crisis_hotlines TEXT NULL COMMENT '危机热线（JSON数组）',
    reasons_to_live TEXT NULL COMMENT '活着的理由',
    last_reviewed_at DATETIME NULL COMMENT '上次审查时间',
    last_reviewed_by BIGINT NULL COMMENT '上次审查人',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_safety_plan_patient (patient_id, plan_status),
    CONSTRAINT fk_safety_plan_patient FOREIGN KEY (patient_id) REFERENCES user(id),
    CONSTRAINT fk_safety_plan_creator FOREIGN KEY (created_by) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者安全计划';

-- 4) 危机联系尝试记录
CREATE TABLE crisis_contact_attempt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    crisis_case_id BIGINT NOT NULL COMMENT '危机案例ID',
    contact_target VARCHAR(32) NOT NULL COMMENT 'PATIENT,EMERGENCY_CONTACT,DOCTOR,ADMIN',
    contact_channel VARCHAR(32) NOT NULL COMMENT 'PHONE,SMS,IN_APP,EMAIL',
    contact_name VARCHAR(100) NULL COMMENT '联系人姓名',
    contact_info VARCHAR(200) NULL COMMENT '联系方式',
    attempt_status VARCHAR(32) NOT NULL DEFAULT 'ATTEMPTED' COMMENT 'ATTEMPTED,REACHED,NO_ANSWER,FAILED',
    attempted_at DATETIME NOT NULL COMMENT '尝试时间',
    attempted_by BIGINT NULL COMMENT '尝试人',
    duration_seconds INT NULL COMMENT '通话时长',
    outcome_note VARCHAR(500) NULL COMMENT '结果备注',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contact_case (crisis_case_id, attempted_at),
    CONSTRAINT fk_contact_case FOREIGN KEY (crisis_case_id) REFERENCES crisis_case(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='危机联系尝试';

-- 5) 扩展现有表
ALTER TABLE risk_event
    ADD COLUMN crisis_case_id BIGINT NULL COMMENT '归属危机案例ID' AFTER suppressed_by_alert_id,
    ADD COLUMN window_start DATETIME NULL COMMENT '聚合窗口开始' AFTER crisis_case_id,
    ADD COLUMN window_end DATETIME NULL COMMENT '聚合窗口结束' AFTER window_start,
    ADD COLUMN confidence DECIMAL(5,2) NULL COMMENT '置信度' AFTER window_end,
    ADD COLUMN explanation_json JSON NULL COMMENT '解释' AFTER confidence,
    ADD INDEX idx_risk_event_case (crisis_case_id),
    ADD CONSTRAINT fk_risk_event_case FOREIGN KEY (crisis_case_id) REFERENCES crisis_case(id);

ALTER TABLE crisis_alert
    ADD COLUMN crisis_case_id BIGINT NULL COMMENT '归属危机案例ID' AFTER risk_event_id,
    ADD COLUMN owner_doctor_id BIGINT NULL COMMENT '主责医生ID' AFTER crisis_case_id,
    ADD COLUMN triage_level VARCHAR(16) NULL COMMENT '分诊等级' AFTER owner_doctor_id,
    ADD COLUMN reopen_count INT NOT NULL DEFAULT 0 COMMENT '重开次数' AFTER triage_level,
    ADD COLUMN next_action_deadline DATETIME NULL COMMENT '下一步截止' AFTER reopen_count,
    ADD COLUMN closed_reason VARCHAR(32) NULL COMMENT '关闭原因' AFTER resolution_note,
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER closed_reason,
    ADD INDEX idx_alert_case (crisis_case_id),
    ADD CONSTRAINT fk_alert_case FOREIGN KEY (crisis_case_id) REFERENCES crisis_case(id);
