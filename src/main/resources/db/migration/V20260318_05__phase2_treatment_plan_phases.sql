-- ============================================================
-- Phase 2 Upgrade Slice 1: 治疗计划阶段化 + 评审 + 修订 + 干预任务
-- ============================================================

-- 1) 治疗计划阶段
CREATE TABLE treatment_plan_phase (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT NOT NULL COMMENT '治疗计划ID',
    phase_code VARCHAR(32) NOT NULL COMMENT 'INTAKE,STABILIZATION,ACTIVE_WORK,MAINTENANCE,DISCHARGE',
    phase_name VARCHAR(128) NOT NULL COMMENT '阶段名称',
    phase_order INT NOT NULL COMMENT '阶段顺序',
    phase_status VARCHAR(32) NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED,ACTIVE,COMPLETED,CANCELLED,SKIPPED',
    entry_criteria TEXT NULL COMMENT '进入条件描述',
    exit_criteria TEXT NULL COMMENT '退出条件描述',
    required_assessments_json JSON NULL COMMENT '本阶段必做量表',
    required_interventions_json JSON NULL COMMENT '本阶段必做干预',
    max_risk_level VARCHAR(16) NULL COMMENT '允许的最大风险等级',
    planned_start_date DATE NULL COMMENT '计划开始日期',
    planned_end_date DATE NULL COMMENT '计划结束日期',
    actual_start_date DATE NULL COMMENT '实际开始日期',
    actual_end_date DATE NULL COMMENT '实际结束日期',
    activated_by BIGINT NULL COMMENT '激活人',
    completed_by BIGINT NULL COMMENT '完成人',
    completion_note VARCHAR(500) NULL COMMENT '完成备注',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phase_plan_order (plan_id, phase_order),
    INDEX idx_phase_status (plan_id, phase_status),
    CONSTRAINT fk_phase_plan FOREIGN KEY (plan_id) REFERENCES treatment_plan(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='治疗计划阶段';

-- 2) 治疗计划评审
CREATE TABLE treatment_plan_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT NOT NULL COMMENT '治疗计划ID',
    phase_id BIGINT NULL COMMENT '关联阶段ID',
    review_type VARCHAR(32) NOT NULL COMMENT 'SCHEDULED,TRIGGERED,MANUAL,CRISIS',
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,IN_PROGRESS,COMPLETED,REOPENED',
    due_at DATETIME NOT NULL COMMENT '截止时间',
    reviewer_id BIGINT NULL COMMENT '评审人',
    started_at DATETIME NULL COMMENT '开始时间',
    completed_at DATETIME NULL COMMENT '完成时间',
    conclusion_code VARCHAR(32) NULL COMMENT 'CONTINUE,ADJUST,ESCALATE,REFER,DISCHARGE',
    summary_text TEXT NULL COMMENT '评审摘要',
    action_plan_json JSON NULL COMMENT '后续行动计划',
    trigger_source_type VARCHAR(32) NULL COMMENT '触发来源类型',
    trigger_source_id BIGINT NULL COMMENT '触发来源ID',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_review_plan_status (plan_id, review_status, due_at),
    INDEX idx_review_reviewer (reviewer_id, review_status),
    CONSTRAINT fk_review_plan FOREIGN KEY (plan_id) REFERENCES treatment_plan(id),
    CONSTRAINT fk_review_phase FOREIGN KEY (phase_id) REFERENCES treatment_plan_phase(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='治疗计划评审';

-- 3) 治疗计划修订
CREATE TABLE treatment_plan_revision (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT NOT NULL COMMENT '治疗计划ID',
    from_version INT NOT NULL COMMENT '修订前版本',
    to_version INT NOT NULL COMMENT '修订后版本',
    revision_status VARCHAR(32) NOT NULL DEFAULT 'PROPOSED' COMMENT 'PROPOSED,APPROVED,APPLIED,REJECTED',
    proposed_by BIGINT NOT NULL COMMENT '提出人',
    proposed_at DATETIME NOT NULL COMMENT '提出时间',
    approved_by BIGINT NULL COMMENT '批准人',
    approved_at DATETIME NULL COMMENT '批准时间',
    applied_at DATETIME NULL COMMENT '生效时间',
    change_summary TEXT NOT NULL COMMENT '变更摘要',
    change_detail_json JSON NULL COMMENT '变更明细(before/after)',
    revision_reason VARCHAR(500) NULL COMMENT '修订原因',
    review_id BIGINT NULL COMMENT '关联评审ID',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_revision_plan (plan_id, revision_status),
    CONSTRAINT fk_revision_plan FOREIGN KEY (plan_id) REFERENCES treatment_plan(id),
    CONSTRAINT fk_revision_review FOREIGN KEY (review_id) REFERENCES treatment_plan_review(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='治疗计划修订';

-- 4) 干预任务（给患者的家庭作业/冥想/记录任务）
CREATE TABLE intervention_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT NOT NULL COMMENT '治疗计划ID',
    phase_id BIGINT NULL COMMENT '所属阶段ID',
    intervention_id BIGINT NULL COMMENT '关联干预ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    assigned_by BIGINT NOT NULL COMMENT '分配人',
    task_type VARCHAR(32) NOT NULL COMMENT 'HOMEWORK,MEDITATION,JOURNALING,EXERCISE,READING,CUSTOM',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    description TEXT NULL COMMENT '任务说明',
    task_status VARCHAR(32) NOT NULL DEFAULT 'ASSIGNED' COMMENT 'ASSIGNED,IN_PROGRESS,SUBMITTED,REVIEWED,COMPLETED,OVERDUE,CANCELLED',
    assigned_at DATETIME NOT NULL COMMENT '分配时间',
    due_at DATETIME NOT NULL COMMENT '截止时间',
    started_at DATETIME NULL COMMENT '开始时间',
    submitted_at DATETIME NULL COMMENT '提交时间',
    submission_note TEXT NULL COMMENT '提交备注',
    submission_value VARCHAR(200) NULL COMMENT '提交数值(如冥想分钟数)',
    reviewed_by BIGINT NULL COMMENT '复核人',
    reviewed_at DATETIME NULL COMMENT '复核时间',
    review_result_code VARCHAR(32) NULL COMMENT 'APPROVED,REDO,NOTED',
    review_note VARCHAR(500) NULL COMMENT '复核备注',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_patient_status (patient_id, task_status, due_at),
    INDEX idx_task_plan_phase (plan_id, phase_id, task_status),
    CONSTRAINT fk_task_plan FOREIGN KEY (plan_id) REFERENCES treatment_plan(id),
    CONSTRAINT fk_task_phase FOREIGN KEY (phase_id) REFERENCES treatment_plan_phase(id),
    CONSTRAINT fk_task_patient FOREIGN KEY (patient_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='干预任务';

-- 5) 扩展现有表
ALTER TABLE treatment_plan
    ADD COLUMN current_phase_id BIGINT NULL COMMENT '当前阶段ID' AFTER plan_status,
    ADD COLUMN review_interval_days INT NOT NULL DEFAULT 14 COMMENT '评审间隔天数' AFTER plan_version,
    ADD COLUMN next_review_at DATETIME NULL COMMENT '下次评审时间' AFTER review_interval_days,
    ADD COLUMN adherence_score DECIMAL(5,2) NULL COMMENT '依从性评分' AFTER next_review_at,
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER adherence_score;

ALTER TABLE treatment_goal
    ADD COLUMN phase_id BIGINT NULL COMMENT '所属阶段ID' AFTER plan_id,
    ADD COLUMN baseline_value VARCHAR(100) NULL COMMENT '基线值' AFTER progress_pct,
    ADD COLUMN current_value VARCHAR(100) NULL COMMENT '当前值' AFTER baseline_value,
    ADD COLUMN measurement_unit VARCHAR(50) NULL COMMENT '度量单位' AFTER current_value,
    ADD COLUMN stagnation_days INT NOT NULL DEFAULT 0 COMMENT '停滞天数' AFTER measurement_unit,
    ADD COLUMN last_progress_at DATETIME NULL COMMENT '最近进展时间' AFTER achieved_at,
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER last_progress_at;

ALTER TABLE treatment_intervention
    ADD COLUMN phase_id BIGINT NULL COMMENT '所属阶段ID' AFTER goal_id,
    ADD COLUMN task_required_flag VARCHAR(8) NOT NULL DEFAULT 'N' COMMENT '是否需落任务 Y/N' AFTER frequency,
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER status;

ALTER TABLE session_note
    ADD COLUMN phase_id BIGINT NULL COMMENT '所属阶段ID' AFTER plan_id,
    ADD COLUMN review_id BIGINT NULL COMMENT '关联评审ID' AFTER phase_id,
    ADD COLUMN review_required_flag VARCHAR(8) NOT NULL DEFAULT 'N' COMMENT '是否需评审 Y/N' AFTER next_session_plan,
    ADD COLUMN follow_up_due_at DATETIME NULL COMMENT '后续动作截止' AFTER review_required_flag,
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER follow_up_due_at;
