-- ============================================================
-- Phase 3 Upgrade Slice 2: 日记洞察 + 冥想疗效追踪
-- ============================================================

-- 1) 日记特征抽取结果
CREATE TABLE mood_diary_feature (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    mood_diary_id BIGINT NOT NULL COMMENT '日记ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    extraction_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,SUCCESS,FAILED,REVIEWED',
    sentiment_score DECIMAL(10,2) NULL COMMENT '情感分数(-1到1)',
    primary_emotion_code VARCHAR(32) NULL COMMENT '主情绪编码',
    emotion_tags_json JSON NULL COMMENT '情绪标签',
    trigger_tags_json JSON NULL COMMENT '触发因素标签',
    coping_tags_json JSON NULL COMMENT '应对方式标签',
    risk_signal_json JSON NULL COMMENT '风险信号',
    ai_summary VARCHAR(500) NULL COMMENT 'AI摘要',
    extracted_at DATETIME NULL COMMENT '抽取时间',
    reviewed_by BIGINT NULL COMMENT '复核人',
    reviewed_at DATETIME NULL COMMENT '复核时间',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_diary_feature (mood_diary_id),
    INDEX idx_feature_user_status (user_id, extraction_status),
    CONSTRAINT fk_feature_diary FOREIGN KEY (mood_diary_id) REFERENCES mood_diary(id),
    CONSTRAINT fk_feature_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记特征抽取';

-- 2) 冥想处方
CREATE TABLE meditation_prescription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    treatment_plan_id BIGINT NULL COMMENT '治疗计划ID',
    exercise_id BIGINT NOT NULL COMMENT '练习ID',
    prescription_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT,ACTIVE,PAUSED,COMPLETED,CANCELLED',
    goal_code VARCHAR(32) NOT NULL COMMENT 'ANXIETY_RELIEF,SLEEP_SUPPORT,GROUNDING,MAINTENANCE',
    frequency_code VARCHAR(32) NOT NULL DEFAULT 'DAILY' COMMENT 'DAILY,WEEKLY,CUSTOM',
    sessions_per_week INT NOT NULL DEFAULT 3 COMMENT '每周次数',
    minutes_per_session INT NOT NULL DEFAULT 10 COMMENT '每次分钟数',
    start_at DATETIME NOT NULL COMMENT '开始时间',
    end_at DATETIME NULL COMMENT '结束时间',
    next_due_at DATETIME NULL COMMENT '下次建议时间',
    total_sessions_completed INT NOT NULL DEFAULT 0 COMMENT '完成总次数',
    avg_effect_score DECIMAL(5,2) NULL COMMENT '平均效果评分',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_prescription_patient (patient_id, prescription_status),
    INDEX idx_prescription_doctor (doctor_id, prescription_status),
    CONSTRAINT fk_prescription_patient FOREIGN KEY (patient_id) REFERENCES user(id),
    CONSTRAINT fk_prescription_doctor FOREIGN KEY (doctor_id) REFERENCES user(id),
    CONSTRAINT fk_prescription_exercise FOREIGN KEY (exercise_id) REFERENCES meditation_exercise(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='冥想处方';

-- 3) 冥想疗效记录
CREATE TABLE meditation_effect_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    meditation_session_id BIGINT NOT NULL COMMENT '冥想会话ID',
    meditation_prescription_id BIGINT NULL COMMENT '冥想处方ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    effect_status VARCHAR(32) NOT NULL DEFAULT 'RECORDED' COMMENT 'RECORDED,REVIEWED',
    pre_mood_score INT NULL COMMENT '前心情分(1-10)',
    post_mood_score INT NULL COMMENT '后心情分(1-10)',
    pre_stress_score INT NULL COMMENT '前压力分(1-10)',
    post_stress_score INT NULL COMMENT '后压力分(1-10)',
    mood_change INT NULL COMMENT '心情变化(后-前)',
    stress_change INT NULL COMMENT '压力变化(后-前)',
    perceived_benefit_score INT NULL COMMENT '自评收益(1-5)',
    note VARCHAR(500) NULL COMMENT '备注',
    recorded_at DATETIME NOT NULL COMMENT '记录时间',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_effect_patient_time (patient_id, recorded_at),
    INDEX idx_effect_prescription (meditation_prescription_id, effect_status),
    CONSTRAINT fk_effect_session FOREIGN KEY (meditation_session_id) REFERENCES meditation_session(id),
    CONSTRAINT fk_effect_patient FOREIGN KEY (patient_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='冥想疗效记录';

-- 4) 扩展mood_diary
ALTER TABLE mood_diary
    ADD COLUMN sentiment_score DECIMAL(10,2) NULL COMMENT '情感分数' AFTER content,
    ADD COLUMN feature_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,SUCCESS,FAILED' AFTER sentiment_score,
    ADD COLUMN emotion_tags_json JSON NULL COMMENT '情绪标签' AFTER feature_status,
    ADD COLUMN trigger_tags_json JSON NULL COMMENT '触发因素' AFTER emotion_tags_json,
    ADD COLUMN coping_tags_json JSON NULL COMMENT '应对方式' AFTER trigger_tags_json,
    ADD COLUMN ai_summary VARCHAR(500) NULL COMMENT 'AI摘要' AFTER coping_tags_json;

-- 5) 扩展meditation_session
ALTER TABLE meditation_session
    ADD COLUMN meditation_prescription_id BIGINT NULL COMMENT '冥想处方ID' AFTER exercise_id,
    ADD COLUMN pre_mood_score INT NULL COMMENT '前心情分' AFTER planned_seconds,
    ADD COLUMN post_mood_score INT NULL COMMENT '后心情分' AFTER pre_mood_score,
    ADD COLUMN pre_stress_score INT NULL COMMENT '前压力分' AFTER post_mood_score,
    ADD COLUMN post_stress_score INT NULL COMMENT '后压力分' AFTER pre_stress_score,
    ADD COLUMN completion_rate DECIMAL(10,2) NULL COMMENT '完成率' AFTER actual_seconds,
    ADD COLUMN source_plan_id BIGINT NULL COMMENT '来源治疗计划ID' AFTER completion_rate;
