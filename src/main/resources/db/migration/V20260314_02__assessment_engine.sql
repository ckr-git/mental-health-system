-- ============================================================
-- Phase 2: 结构化心理评估引擎
-- ============================================================

-- 1) 量表定义（版本化、不可变）
CREATE TABLE assessment_scale (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scale_code VARCHAR(30) NOT NULL COMMENT 'PHQ9,GAD7,SDS,SAS',
    scale_name VARCHAR(100) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    scale_type VARCHAR(30) NOT NULL COMMENT 'SCREENING,SELF_REPORT',
    intro_text TEXT NULL,
    scoring_rule JSON NOT NULL,
    interpretation_rule JSON NOT NULL,
    active TINYINT NOT NULL DEFAULT 1,
    estimated_minutes INT NOT NULL DEFAULT 5,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_scale_code_version (scale_code, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估量表定义';

-- 2) 量表题目
CREATE TABLE assessment_scale_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scale_id BIGINT NOT NULL,
    item_no INT NOT NULL,
    item_code VARCHAR(30) NOT NULL,
    question_text VARCHAR(500) NOT NULL,
    answer_options JSON NOT NULL COMMENT '[{"label":"完全没有","value":0}]',
    reverse_scored TINYINT NOT NULL DEFAULT 0,
    dimension_code VARCHAR(30) NULL,
    required_flag TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_scale_item_no (scale_id, item_no),
    CONSTRAINT fk_scale_item_scale FOREIGN KEY (scale_id) REFERENCES assessment_scale(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估量表题目';

-- 3) 答题会话
CREATE TABLE assessment_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    scale_id BIGINT NOT NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'SELF' COMMENT 'SELF,DOCTOR_ASSIGNED',
    session_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS,COMPLETED,ABANDONED',
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at DATETIME NULL,
    scored_at DATETIME NULL,
    total_score INT NULL,
    severity_level VARCHAR(30) NULL,
    score_breakdown JSON NULL,
    interpretation TEXT NULL,
    report_id BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session_user_status (user_id, session_status, started_at),
    CONSTRAINT fk_session_scale FOREIGN KEY (scale_id) REFERENCES assessment_scale(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估答题会话';

-- 4) 答题记录
CREATE TABLE assessment_response (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    answer_value INT NOT NULL,
    answer_label VARCHAR(100) NULL,
    answer_text VARCHAR(500) NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_session_item (session_id, item_id),
    CONSTRAINT fk_response_session FOREIGN KEY (session_id) REFERENCES assessment_session(id),
    CONSTRAINT fk_response_item FOREIGN KEY (item_id) REFERENCES assessment_scale_item(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估答题记录';

-- 5) 扩展已有 assessment_report 表
ALTER TABLE assessment_report
    ADD COLUMN assessment_session_id BIGINT NULL,
    ADD COLUMN scale_code VARCHAR(30) NULL,
    ADD COLUMN severity_level VARCHAR(30) NULL,
    ADD COLUMN raw_score_json JSON NULL;

-- ============================================================
-- 种子数据：PHQ-9 抑郁筛查量表
-- ============================================================
INSERT INTO assessment_scale (scale_code, scale_name, version, scale_type, intro_text, scoring_rule, interpretation_rule, active, estimated_minutes)
VALUES ('PHQ9', 'PHQ-9 抑郁筛查量表', 1, 'SCREENING',
    '患者健康问卷-9（PHQ-9）是一种简短的自评工具，用于筛查和评估抑郁症状的严重程度。请根据过去2周的情况，选择最符合您状况的选项。',
    '{"method":"SUM","scoreField":"answerValue"}',
    '{"ranges":[{"min":0,"max":4,"label":"极轻微","text":"您目前的抑郁症状极轻微，建议保持规律作息和情绪观察。"},{"min":5,"max":9,"label":"轻度","text":"您可能存在轻度抑郁症状，建议结合日记记录、适量运动与社交支持持续关注。"},{"min":10,"max":14,"label":"中度","text":"您的抑郁症状已达中度，建议及时与专业医师或心理咨询师进行沟通。"},{"min":15,"max":19,"label":"中重度","text":"您的抑郁症状较为明显，建议尽快安排专业评估和治疗。"},{"min":20,"max":27,"label":"重度","text":"您的抑郁症状较为严重，请尽快寻求专业医疗与心理支持。"}]}',
    1, 5);

SET @phq9_id = (SELECT id FROM assessment_scale WHERE scale_code = 'PHQ9' AND version = 1);

INSERT INTO assessment_scale_item (scale_id, item_no, item_code, question_text, answer_options, reverse_scored, dimension_code, required_flag) VALUES
(@phq9_id, 1, 'PHQ9_1', '做事时提不起劲或没有兴趣', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'INTEREST', 1),
(@phq9_id, 2, 'PHQ9_2', '感到心情低落、沮丧和绝望', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'MOOD', 1),
(@phq9_id, 3, 'PHQ9_3', '入睡困难、睡不安或睡眠过多', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'SLEEP', 1),
(@phq9_id, 4, 'PHQ9_4', '感觉疲倦或没有活力', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'ENERGY', 1),
(@phq9_id, 5, 'PHQ9_5', '食欲不振或吃多了', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'APPETITE', 1),
(@phq9_id, 6, 'PHQ9_6', '觉得自己很糟糕，或觉得自己很失败，或让自己或家人失望', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'SELF_ESTEEM', 1),
(@phq9_id, 7, 'PHQ9_7', '对事物专注有困难，例如阅读报纸或看电视时', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'CONCENTRATION', 1),
(@phq9_id, 8, 'PHQ9_8', '动作或说话速度缓慢到别人已经察觉；或者恰恰相反，烦躁或坐立不安', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'PSYCHOMOTOR', 1),
(@phq9_id, 9, 'PHQ9_9', '有不如死掉或用某种方式伤害自己的念头', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'RISK', 1);

-- ============================================================
-- 种子数据：GAD-7 焦虑筛查量表
-- ============================================================
INSERT INTO assessment_scale (scale_code, scale_name, version, scale_type, intro_text, scoring_rule, interpretation_rule, active, estimated_minutes)
VALUES ('GAD7', 'GAD-7 焦虑筛查量表', 1, 'SCREENING',
    '广泛性焦虑量表-7（GAD-7）是一种简短的自评工具，用于筛查和评估焦虑症状的严重程度。请根据过去2周的情况，选择最符合您状况的选项。',
    '{"method":"SUM","scoreField":"answerValue"}',
    '{"ranges":[{"min":0,"max":4,"label":"极轻微","text":"您目前的焦虑症状极轻微，建议保持当前的生活节奏与放松方式。"},{"min":5,"max":9,"label":"轻度","text":"您可能存在轻度焦虑症状，建议增加放松训练与睡眠管理。"},{"min":10,"max":14,"label":"中度","text":"您的焦虑症状已达中度，建议与专业人员进行评估咨询。"},{"min":15,"max":21,"label":"重度","text":"您的焦虑症状较为严重，请尽快寻求专业帮助和干预。"}]}',
    1, 4);

SET @gad7_id = (SELECT id FROM assessment_scale WHERE scale_code = 'GAD7' AND version = 1);

INSERT INTO assessment_scale_item (scale_id, item_no, item_code, question_text, answer_options, reverse_scored, dimension_code, required_flag) VALUES
(@gad7_id, 1, 'GAD7_1', '感到紧张、焦虑或急迫', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'ANXIETY', 1),
(@gad7_id, 2, 'GAD7_2', '无法停止或控制担忧', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'WORRY', 1),
(@gad7_id, 3, 'GAD7_3', '对各种各样的事情担忧过多', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'WORRY', 1),
(@gad7_id, 4, 'GAD7_4', '很难放松下来', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'RELAXATION', 1),
(@gad7_id, 5, 'GAD7_5', '由于不安而无法静坐', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'RESTLESSNESS', 1),
(@gad7_id, 6, 'GAD7_6', '容易变得烦闷或易怒', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'IRRITABILITY', 1),
(@gad7_id, 7, 'GAD7_7', '感到好像将有可怕的事情发生', '[{"label":"完全没有","value":0},{"label":"几天","value":1},{"label":"一半以上天数","value":2},{"label":"几乎每天","value":3}]', 0, 'FEAR', 1);
