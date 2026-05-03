-- ============================================================
-- Phase 3 Upgrade Slice 1: 多维分析引擎
-- 患者日粒度指标、医生工作负载快照、分析作业日志
-- ============================================================

-- 1) 患者日粒度指标
CREATE TABLE patient_daily_metric (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    metric_at DATETIME NOT NULL COMMENT '统计日期(归一到00:00:00)',
    metric_status VARCHAR(32) NOT NULL DEFAULT 'CALCULATED' COMMENT 'CALCULATED,LOCKED,RECALCULATED',
    mood_avg DECIMAL(10,2) NULL COMMENT '平均心情',
    sleep_avg DECIMAL(10,2) NULL COMMENT '平均睡眠',
    stress_avg DECIMAL(10,2) NULL COMMENT '平均压力',
    energy_avg DECIMAL(10,2) NULL COMMENT '平均精力',
    diary_count INT NOT NULL DEFAULT 0 COMMENT '日记数',
    latest_phq9_score INT NULL COMMENT '最新PHQ9',
    latest_gad7_score INT NULL COMMENT '最新GAD7',
    task_completion_rate DECIMAL(10,2) NULL COMMENT '任务完成率',
    meditation_minutes INT NOT NULL DEFAULT 0 COMMENT '冥想分钟数',
    risk_event_count INT NOT NULL DEFAULT 0 COMMENT '风险事件数',
    no_show_count INT NOT NULL DEFAULT 0 COMMENT '爽约数',
    source_window_start DATETIME NOT NULL COMMENT '统计窗口开始',
    source_window_end DATETIME NOT NULL COMMENT '统计窗口结束',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_patient_daily_metric (patient_id, metric_at),
    INDEX idx_patient_daily_metric_time (patient_id, metric_at),
    CONSTRAINT fk_pdm_patient FOREIGN KEY (patient_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者日粒度指标';

-- 2) 医生工作负载快照
CREATE TABLE doctor_workload_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    snapshot_at DATETIME NOT NULL COMMENT '统计日期(归一到00:00:00)',
    snapshot_status VARCHAR(32) NOT NULL DEFAULT 'CALCULATED' COMMENT 'CALCULATED,LOCKED,RECALCULATED',
    scheduled_appointments INT NOT NULL DEFAULT 0 COMMENT '预约数',
    completed_appointments INT NOT NULL DEFAULT 0 COMMENT '完成数',
    no_show_count INT NOT NULL DEFAULT 0 COMMENT '爽约数',
    crisis_open_count INT NOT NULL DEFAULT 0 COMMENT '打开危机案例数',
    crisis_overdue_count INT NOT NULL DEFAULT 0 COMMENT '逾期危机数',
    active_plans INT NOT NULL DEFAULT 0 COMMENT '进行中计划数',
    active_referrals INT NOT NULL DEFAULT 0 COMMENT '进行中转诊数',
    pending_reviews INT NOT NULL DEFAULT 0 COMMENT '待评审数',
    pending_handoff_tasks INT NOT NULL DEFAULT 0 COMMENT '待处理转人工数',
    waitlist_pending_count INT NOT NULL DEFAULT 0 COMMENT '待处理候补数',
    avg_response_hours DECIMAL(10,2) NULL COMMENT '平均响应时长',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doctor_workload (doctor_id, snapshot_at),
    INDEX idx_doctor_workload_time (doctor_id, snapshot_at),
    CONSTRAINT fk_dws_doctor FOREIGN KEY (doctor_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生工作负载快照';

-- 3) 分析作业日志
CREATE TABLE analytics_job_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    job_code VARCHAR(64) NOT NULL COMMENT '作业编码',
    job_scope_code VARCHAR(32) NOT NULL COMMENT 'PATIENT_DAILY,DOCTOR_DAILY,OUTCOME_REFRESH,FEATURE_EXTRACTION',
    job_status VARCHAR(32) NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED,RUNNING,SUCCESS,FAILED,PARTIAL,CANCELLED',
    target_date DATETIME NOT NULL COMMENT '目标日期',
    started_at DATETIME NULL COMMENT '开始时间',
    finished_at DATETIME NULL COMMENT '结束时间',
    records_processed INT NOT NULL DEFAULT 0 COMMENT '处理记录数',
    error_message VARCHAR(1000) NULL COMMENT '错误消息',
    result_summary_json JSON NULL COMMENT '结果摘要',
    triggered_by BIGINT NULL COMMENT '触发人(NULL=系统)',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_job_scope_status (job_scope_code, job_status, target_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析作业日志';

-- 4) 扩展outcome_snapshot
ALTER TABLE outcome_snapshot
    ADD COLUMN delta_from_baseline_phq9 DECIMAL(10,2) NULL COMMENT '相对基线PHQ9变化' AFTER gad7_severity,
    ADD COLUMN delta_from_baseline_gad7 DECIMAL(10,2) NULL COMMENT '相对基线GAD7变化' AFTER delta_from_baseline_phq9,
    ADD COLUMN adherence_score DECIMAL(10,2) NULL COMMENT '依从性评分' AFTER avg_goal_progress,
    ADD COLUMN no_show_rate DECIMAL(10,2) NULL COMMENT '爽约率' AFTER adherence_score,
    ADD COLUMN relapse_risk_level VARCHAR(16) NULL COMMENT '复发风险等级' AFTER no_show_rate,
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER clinician_notes;

-- 5) 扩展doctor_performance
ALTER TABLE doctor_performance
    ADD COLUMN waitlist_conversion_rate DECIMAL(10,2) NULL COMMENT '候补转化率' AFTER appointments_completed,
    ADD COLUMN no_show_rate DECIMAL(10,2) NULL COMMENT '爽约率' AFTER waitlist_conversion_rate,
    ADD COLUMN avg_follow_up_gap_days DECIMAL(10,2) NULL COMMENT '平均随访间隔天数' AFTER no_show_rate,
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER avg_response_time_hours;
