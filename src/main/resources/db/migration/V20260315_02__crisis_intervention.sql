-- ============================================================
-- Slice 2: 危机干预 schema + 风险规则种子数据
-- ============================================================

-- 1) 风险规则表
CREATE TABLE risk_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    source_type VARCHAR(32) NOT NULL COMMENT 'DIARY,CHAT,ASSESSMENT,TREND',
    matcher_type VARCHAR(32) NOT NULL COMMENT 'KEYWORD,PHQ9_Q9,TREND_THRESHOLD',
    keyword_pattern VARCHAR(500) NULL,
    threshold_config JSON NULL,
    risk_level VARCHAR(16) NOT NULL COMMENT 'LOW,MEDIUM,HIGH,CRITICAL',
    score_weight INT NOT NULL DEFAULT 0,
    cooldown_minutes INT NOT NULL DEFAULT 240,
    active TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_risk_rule_code (rule_code),
    INDEX idx_rule_source_active (source_type, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险检测规则';

-- 2) 风险事件
CREATE TABLE risk_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    content_snapshot TEXT NULL,
    matched_rules JSON NULL,
    computed_score INT NOT NULL DEFAULT 0,
    computed_level VARCHAR(16) NOT NULL,
    decision VARCHAR(32) NOT NULL COMMENT 'NO_ALERT,ALERTED,SUPPRESSED',
    suppressed_by_alert_id BIGINT NULL,
    detected_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_risk_event_user_time (user_id, detected_at),
    INDEX idx_risk_event_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险事件记录';

-- 3) 危机警报
CREATE TABLE crisis_alert (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    doctor_id BIGINT NULL,
    risk_event_id BIGINT NOT NULL,
    alert_level VARCHAR(16) NOT NULL COMMENT 'MEDIUM,HIGH,CRITICAL',
    alert_status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN,ACKNOWLEDGED,ESCALATED,RESOLVED,SUPPRESSED',
    title VARCHAR(200) NOT NULL,
    summary TEXT NOT NULL,
    evidence_json JSON NULL,
    sla_deadline DATETIME NOT NULL,
    acknowledged_by BIGINT NULL,
    acknowledged_at DATETIME NULL,
    resolved_by BIGINT NULL,
    resolved_at DATETIME NULL,
    resolution_note VARCHAR(500) NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_alert_event (risk_event_id),
    INDEX idx_alert_doctor_status (doctor_id, alert_status, sla_deadline),
    INDEX idx_alert_user_status (user_id, alert_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危机警报';

-- 4) 处置记录
CREATE TABLE crisis_alert_action (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL COMMENT 'CREATED,ACKNOWLEDGED,ESCALATED,RESOLVED,SUPPRESSED',
    operator_id BIGINT NULL,
    operator_role VARCHAR(16) NULL COMMENT 'SYSTEM,DOCTOR,ADMIN',
    action_note VARCHAR(500) NULL,
    metadata JSON NULL,
    action_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_action_alert_time (alert_id, action_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危机处置记录';

-- ============================================================
-- 风险规则种子数据
-- ============================================================
INSERT INTO risk_rule (rule_code, rule_name, source_type, matcher_type, keyword_pattern, risk_level, score_weight, cooldown_minutes) VALUES
('KEYWORD_SUICIDE', '自杀/自伤关键词', 'DIARY', 'KEYWORD', '自杀,想死,不想活,活不下去,结束生命,去死,跳楼,割腕,吃药自杀,上吊', 'CRITICAL', 40, 240),
('KEYWORD_SUICIDE_CHAT', '聊天自杀关键词', 'CHAT', 'KEYWORD', '自杀,想死,不想活,活不下去,结束生命,去死,跳楼,割腕', 'CRITICAL', 40, 240),
('KEYWORD_DESPAIR', '绝望/无助表达', 'DIARY', 'KEYWORD', '绝望,没有希望,活着没意义,没人关心,世界不需要我,累了,撑不下去', 'HIGH', 20, 240),
('KEYWORD_DESPAIR_CHAT', '聊天绝望表达', 'CHAT', 'KEYWORD', '绝望,没有希望,活着没意义,没人关心,世界不需要我,撑不下去', 'HIGH', 20, 240),
('KEYWORD_SELFHARM', '自伤行为描述', 'DIARY', 'KEYWORD', '伤害自己,划伤,烧伤自己,打自己,撞墙,拔头发', 'HIGH', 20, 240),
('PHQ9_Q9_RISK', 'PHQ-9第9题风险', 'ASSESSMENT', 'PHQ9_Q9', NULL, 'HIGH', 30, 240),
('TREND_LOW_MOOD', '持续低心情趋势', 'TREND', 'TREND_THRESHOLD', NULL, 'MEDIUM', 10, 1440),
('TREND_SLEEP_CRISIS', '睡眠崩溃趋势', 'TREND', 'TREND_THRESHOLD', NULL, 'MEDIUM', 10, 1440);

UPDATE risk_rule SET threshold_config = '{"field":"moodAvg3d","op":"<=","value":3.0}' WHERE rule_code = 'TREND_LOW_MOOD';
UPDATE risk_rule SET threshold_config = '{"field":"sleepAvg3d","op":"<=","value":3.0}' WHERE rule_code = 'TREND_SLEEP_CRISIS';
