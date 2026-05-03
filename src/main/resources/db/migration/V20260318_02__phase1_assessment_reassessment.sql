-- ============================================================
-- Phase 1 Upgrade Slice 2: 评估复评机制
-- 从单次量表升级为评估协议、任务分配、维度评分、基线对比
-- ============================================================

-- 1) 评估协议（定义什么时候给谁发什么量表）
CREATE TABLE assessment_protocol (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    protocol_code VARCHAR(64) NOT NULL COMMENT '协议编码',
    protocol_name VARCHAR(128) NOT NULL COMMENT '协议名称',
    protocol_type VARCHAR(32) NOT NULL COMMENT 'INTAKE,CRISIS_FOLLOWUP,TREATMENT_REVIEW,DISCHARGE,CUSTOM',
    description VARCHAR(500) NULL COMMENT '说明',
    scale_codes_json JSON NOT NULL COMMENT '包含的量表编码列表',
    trigger_condition VARCHAR(32) NOT NULL COMMENT 'MANUAL,ON_INTAKE,ON_CRISIS,ON_PLAN_REVIEW,PERIODIC',
    period_days INT NULL COMMENT '周期天数（PERIODIC时使用）',
    auto_assign_flag VARCHAR(8) NOT NULL DEFAULT 'N' COMMENT '是否自动分配 Y/N',
    reminder_before_hours INT NOT NULL DEFAULT 24 COMMENT '到期前提醒小时数',
    active TINYINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_protocol_code (protocol_code),
    INDEX idx_protocol_type_active (protocol_type, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评估协议';

-- 2) 评估任务分配
CREATE TABLE assessment_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    protocol_id BIGINT NULL COMMENT '来源协议ID',
    scale_id BIGINT NOT NULL COMMENT '量表ID',
    scale_code VARCHAR(64) NOT NULL COMMENT '量表编码',
    assigned_by BIGINT NULL COMMENT '分配人（NULL=系统）',
    assigned_at DATETIME NOT NULL COMMENT '分配时间',
    due_at DATETIME NOT NULL COMMENT '截止时间',
    assignment_status VARCHAR(32) NOT NULL DEFAULT 'ASSIGNED' COMMENT 'ASSIGNED,IN_PROGRESS,SUBMITTED,REVIEWED,OVERDUE,CLOSED,CANCELLED',
    session_id BIGINT NULL COMMENT '关联评估会话ID',
    reminder_sent_count INT NOT NULL DEFAULT 0 COMMENT '已发提醒次数',
    last_reminder_at DATETIME NULL COMMENT '上次提醒时间',
    baseline_flag VARCHAR(8) NOT NULL DEFAULT 'N' COMMENT '是否基线评估 Y/N',
    treatment_plan_id BIGINT NULL COMMENT '关联治疗计划',
    source_event_type VARCHAR(32) NULL COMMENT '触发来源类型',
    source_event_id BIGINT NULL COMMENT '触发来源ID',
    reviewed_by BIGINT NULL COMMENT '复核人',
    reviewed_at DATETIME NULL COMMENT '复核时间',
    review_note VARCHAR(500) NULL COMMENT '复核备注',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_assignment_patient_status (patient_id, assignment_status, due_at),
    INDEX idx_assignment_overdue (assignment_status, due_at),
    INDEX idx_assignment_protocol (protocol_id, assignment_status),
    CONSTRAINT fk_assignment_patient FOREIGN KEY (patient_id) REFERENCES user(id),
    CONSTRAINT fk_assignment_protocol FOREIGN KEY (protocol_id) REFERENCES assessment_protocol(id),
    CONSTRAINT fk_assignment_scale FOREIGN KEY (scale_id) REFERENCES assessment_scale(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评估任务分配';

-- 3) 维度评分结果
CREATE TABLE assessment_dimension_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    session_id BIGINT NOT NULL COMMENT '评估会话ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    scale_code VARCHAR(64) NOT NULL COMMENT '量表编码',
    dimension_code VARCHAR(64) NOT NULL COMMENT '维度编码',
    dimension_name VARCHAR(128) NOT NULL COMMENT '维度名称',
    raw_score INT NOT NULL COMMENT '原始分',
    normalized_score DECIMAL(10,2) NULL COMMENT '标准化分（0-100）',
    severity_level VARCHAR(16) NULL COMMENT '严重等级',
    item_ids_json JSON NULL COMMENT '包含的题目ID列表',
    change_from_baseline DECIMAL(10,2) NULL COMMENT '相对基线变化',
    clinical_significance VARCHAR(32) NULL COMMENT 'IMPROVED,STABLE,DETERIORATED,CLINICALLY_SIGNIFICANT',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_dimension_session (session_id),
    INDEX idx_dimension_patient_scale (patient_id, scale_code, dimension_code, create_time),
    CONSTRAINT fk_dimension_session FOREIGN KEY (session_id) REFERENCES assessment_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='维度评分结果';

-- 4) 评估基线
CREATE TABLE assessment_baseline (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    scale_code VARCHAR(64) NOT NULL COMMENT '量表编码',
    baseline_session_id BIGINT NOT NULL COMMENT '基线会话ID',
    treatment_plan_id BIGINT NULL COMMENT '关联治疗计划',
    total_score INT NOT NULL COMMENT '基线总分',
    dimension_scores_json JSON NULL COMMENT '维度基线分',
    severity_level VARCHAR(16) NOT NULL COMMENT '基线严重等级',
    established_at DATETIME NOT NULL COMMENT '建立时间',
    superseded_at DATETIME NULL COMMENT '被替代时间',
    superseded_by BIGINT NULL COMMENT '替代的基线ID',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_baseline_patient_scale (patient_id, scale_code, superseded_at),
    CONSTRAINT fk_baseline_patient FOREIGN KEY (patient_id) REFERENCES user(id),
    CONSTRAINT fk_baseline_session FOREIGN KEY (baseline_session_id) REFERENCES assessment_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评估基线';

-- 5) 扩展现有assessment_session表
ALTER TABLE assessment_session
    ADD COLUMN assignment_id BIGINT NULL COMMENT '关联任务分配ID' AFTER scale_id,
    ADD COLUMN assigned_by BIGINT NULL COMMENT '分配人' AFTER assignment_id,
    ADD COLUMN assigned_at DATETIME NULL COMMENT '分配时间' AFTER assigned_by,
    ADD COLUMN due_at DATETIME NULL COMMENT '截止时间' AFTER assigned_at,
    ADD COLUMN baseline_flag VARCHAR(8) NOT NULL DEFAULT 'N' COMMENT '是否基线 Y/N' AFTER due_at,
    ADD COLUMN change_from_baseline DECIMAL(10,2) NULL COMMENT '相对基线变化' AFTER baseline_flag,
    ADD COLUMN clinical_flag VARCHAR(32) NULL COMMENT '临床标记' AFTER change_from_baseline,
    ADD INDEX idx_session_assignment (assignment_id);

-- 6) 扩展现有assessment_report表
ALTER TABLE assessment_report
    ADD COLUMN recommendation_code VARCHAR(32) NULL COMMENT '建议代码' AFTER raw_score_json,
    ADD COLUMN follow_up_required VARCHAR(8) NOT NULL DEFAULT 'N' COMMENT '是否需要随访 Y/N' AFTER recommendation_code,
    ADD COLUMN follow_up_due_at DATETIME NULL COMMENT '随访截止' AFTER follow_up_required,
    ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,REVIEWED,ACKNOWLEDGED' AFTER follow_up_due_at,
    ADD COLUMN reviewed_by_doctor BIGINT NULL COMMENT '复核医生' AFTER review_status,
    ADD COLUMN reviewed_at DATETIME NULL COMMENT '复核时间' AFTER reviewed_by_doctor;

-- 7) 种子数据：标准评估协议
INSERT INTO assessment_protocol (protocol_code, protocol_name, protocol_type, scale_codes_json, trigger_condition, period_days, auto_assign_flag) VALUES
('INTAKE_STANDARD', '首诊标准评估包', 'INTAKE', '["PHQ9","GAD7"]', 'ON_INTAKE', NULL, 'Y'),
('CRISIS_FOLLOWUP', '危机后复评', 'CRISIS_FOLLOWUP', '["PHQ9"]', 'ON_CRISIS', NULL, 'Y'),
('TREATMENT_BIWEEKLY', '治疗双周复评', 'TREATMENT_REVIEW', '["PHQ9","GAD7"]', 'PERIODIC', 14, 'Y'),
('DISCHARGE_EVAL', '出院前评估', 'DISCHARGE', '["PHQ9","GAD7"]', 'MANUAL', NULL, 'N');
