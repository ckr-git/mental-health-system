-- ============================================================
-- Phase 2 Upgrade Slice 3: AI会话安全增强
-- 会话线程、多轮消息、摘要记忆、安全判别、转人工
-- ============================================================

-- 1) AI会话（线程级）
CREATE TABLE ai_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    session_type VARCHAR(32) NOT NULL DEFAULT 'SUPPORTIVE' COMMENT 'SUPPORTIVE,PSYCHOEDUCATION,COPING_EXERCISE,CRISIS_SUPPORT',
    session_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE,RISK_REVIEW_REQUIRED,HANDOFF_PENDING,HANDED_OFF,CLOSED',
    topic VARCHAR(200) NULL COMMENT '会话主题',
    message_count INT NOT NULL DEFAULT 0 COMMENT '消息数',
    risk_level VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE,LOW,MEDIUM,HIGH,CRITICAL',
    risk_flags_json JSON NULL COMMENT '风险标记',
    last_message_at DATETIME NULL COMMENT '最后消息时间',
    handoff_task_id BIGINT NULL COMMENT '关联转人工任务ID',
    closed_at DATETIME NULL COMMENT '关闭时间',
    close_reason VARCHAR(32) NULL COMMENT 'USER_CLOSE,TIMEOUT,HANDOFF,SYSTEM',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ai_session_patient (patient_id, session_status, create_time),
    INDEX idx_ai_session_risk (risk_level, session_status),
    CONSTRAINT fk_ai_session_patient FOREIGN KEY (patient_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话';

-- 2) AI消息（多轮）
CREATE TABLE ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    role VARCHAR(16) NOT NULL COMMENT 'USER,ASSISTANT,SYSTEM',
    content TEXT NOT NULL COMMENT '消息内容',
    risk_decision_json JSON NULL COMMENT '风险判别结果',
    emotion_tags_json JSON NULL COMMENT '情绪标签',
    response_type VARCHAR(32) NULL COMMENT 'NORMAL,SAFE_REDIRECT,CRISIS_PROMPT,HANDOFF_NOTICE',
    token_count INT NULL COMMENT 'Token数',
    latency_ms INT NULL COMMENT '响应延迟ms',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ai_message_session (session_id, create_time),
    INDEX idx_ai_message_patient (patient_id, create_time),
    CONSTRAINT fk_ai_message_session FOREIGN KEY (session_id) REFERENCES ai_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI消息';

-- 3) AI摘要记忆
CREATE TABLE ai_memory_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    summary_type VARCHAR(32) NOT NULL COMMENT 'ROLLING,SESSION_END,CROSS_SESSION',
    summary_content TEXT NOT NULL COMMENT '摘要内容',
    key_topics_json JSON NULL COMMENT '关键主题',
    identified_emotions_json JSON NULL COMMENT '识别的情绪',
    identified_triggers_json JSON NULL COMMENT '识别的触发因素',
    suggested_coping_json JSON NULL COMMENT '已建议的应对策略',
    risk_indicators_json JSON NULL COMMENT '风险指标',
    covers_message_from BIGINT NULL COMMENT '覆盖消息起始ID',
    covers_message_to BIGINT NULL COMMENT '覆盖消息结束ID',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_memory_session (session_id, summary_type),
    INDEX idx_memory_patient (patient_id, summary_type, create_time),
    CONSTRAINT fk_memory_session FOREIGN KEY (session_id) REFERENCES ai_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI摘要记忆';

-- 4) AI转人工任务
CREATE TABLE ai_handoff_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    session_id BIGINT NOT NULL COMMENT 'AI会话ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    assigned_doctor_id BIGINT NULL COMMENT '分配的医生ID',
    task_status VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN,ACKNOWLEDGED,IN_PROGRESS,COMPLETED,CANCELLED',
    risk_level VARCHAR(16) NOT NULL COMMENT '风险等级',
    trigger_reason TEXT NOT NULL COMMENT '触发原因',
    trigger_content TEXT NULL COMMENT '触发的用户消息',
    ai_risk_assessment TEXT NULL COMMENT 'AI风险评估摘要',
    session_summary TEXT NULL COMMENT '会话摘要',
    acknowledged_at DATETIME NULL COMMENT '确认时间',
    completed_at DATETIME NULL COMMENT '完成时间',
    completion_note TEXT NULL COMMENT '完成备注',
    follow_up_action VARCHAR(32) NULL COMMENT 'NONE,SCHEDULE_APPOINTMENT,CREATE_CRISIS_CASE,REFER',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_handoff_task_doctor (assigned_doctor_id, task_status),
    INDEX idx_handoff_task_patient (patient_id, task_status),
    INDEX idx_handoff_task_session (session_id),
    CONSTRAINT fk_handoff_session FOREIGN KEY (session_id) REFERENCES ai_session(id),
    CONSTRAINT fk_handoff_patient FOREIGN KEY (patient_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI转人工任务';
