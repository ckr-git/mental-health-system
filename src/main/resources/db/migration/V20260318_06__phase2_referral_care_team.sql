-- ============================================================
-- Phase 2 Upgrade Slice 2: 转诊与护理团队
-- ============================================================

-- 1) 护理团队成员
CREATE TABLE care_team_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    member_role_code VARCHAR(32) NOT NULL COMMENT 'PRIMARY,COLLABORATOR,ON_CALL,COVERAGE,SUPERVISOR',
    member_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE,INACTIVE,REMOVED',
    access_scope_json JSON NULL COMMENT '访问范围 {crisis:true,plan:true,notes:false,assessment:true}',
    coverage_start_at DATETIME NULL COMMENT '覆盖开始时间',
    coverage_end_at DATETIME NULL COMMENT '覆盖结束时间',
    added_by BIGINT NOT NULL COMMENT '添加人',
    added_at DATETIME NOT NULL COMMENT '添加时间',
    removed_at DATETIME NULL COMMENT '移除时间',
    removed_reason VARCHAR(200) NULL COMMENT '移除原因',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_care_team_patient_doctor_role (patient_id, doctor_id, member_role_code, member_status),
    INDEX idx_care_team_patient (patient_id, member_status),
    INDEX idx_care_team_doctor (doctor_id, member_status),
    CONSTRAINT fk_care_team_patient FOREIGN KEY (patient_id) REFERENCES user(id),
    CONSTRAINT fk_care_team_doctor FOREIGN KEY (doctor_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='护理团队成员';

-- 2) 转诊案例
CREATE TABLE referral_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    from_doctor_id BIGINT NOT NULL COMMENT '转出医生ID',
    to_doctor_id BIGINT NULL COMMENT '转入医生ID',
    referral_status VARCHAR(32) NOT NULL DEFAULT 'INITIATED' COMMENT 'INITIATED,ACCEPTED,HANDOFF_READY,HANDOFF_DONE,CLOSED,REJECTED,CANCELLED',
    referral_type VARCHAR(32) NOT NULL COMMENT 'TRANSFER,CONSULTATION,SECOND_OPINION,SPECIALTY',
    urgency_level VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL,URGENT,EMERGENCY',
    reason TEXT NOT NULL COMMENT '转诊原因',
    clinical_summary TEXT NULL COMMENT '临床摘要',
    treatment_plan_id BIGINT NULL COMMENT '关联治疗计划',
    initiated_at DATETIME NOT NULL COMMENT '发起时间',
    accepted_at DATETIME NULL COMMENT '接受时间',
    rejected_at DATETIME NULL COMMENT '拒绝时间',
    reject_reason VARCHAR(500) NULL COMMENT '拒绝原因',
    handoff_ready_at DATETIME NULL COMMENT '交接就绪时间',
    handoff_done_at DATETIME NULL COMMENT '交接完成时间',
    closed_at DATETIME NULL COMMENT '关闭时间',
    closed_by BIGINT NULL COMMENT '关闭人',
    deadline_at DATETIME NULL COMMENT '截止时间',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_referral_patient (patient_id, referral_status),
    INDEX idx_referral_from (from_doctor_id, referral_status),
    INDEX idx_referral_to (to_doctor_id, referral_status),
    CONSTRAINT fk_referral_patient FOREIGN KEY (patient_id) REFERENCES user(id),
    CONSTRAINT fk_referral_from FOREIGN KEY (from_doctor_id) REFERENCES user(id),
    CONSTRAINT fk_referral_to FOREIGN KEY (to_doctor_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='转诊案例';

-- 3) 转诊交接包
CREATE TABLE referral_handoff (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    referral_case_id BIGINT NOT NULL COMMENT '转诊案例ID',
    created_by BIGINT NOT NULL COMMENT '创建人',
    handoff_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT,READY,ACKNOWLEDGED',
    recent_assessments_json JSON NULL COMMENT '近期评估摘要',
    risk_history_json JSON NULL COMMENT '风险历史',
    current_plan_summary TEXT NULL COMMENT '当前计划摘要',
    recent_sessions_summary TEXT NULL COMMENT '近3次会谈摘要',
    medication_summary TEXT NULL COMMENT '用药摘要',
    pending_tasks_json JSON NULL COMMENT '待办事项',
    special_notes TEXT NULL COMMENT '特殊注意事项',
    acknowledged_by BIGINT NULL COMMENT '确认接收人',
    acknowledged_at DATETIME NULL COMMENT '确认时间',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_handoff_case (referral_case_id),
    CONSTRAINT fk_handoff_case FOREIGN KEY (referral_case_id) REFERENCES referral_case(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='转诊交接包';

-- 4) 扩展医患关系表
ALTER TABLE patient_doctor_relationship
    ADD COLUMN relationship_type VARCHAR(32) NOT NULL DEFAULT 'PRIMARY' COMMENT 'PRIMARY,COLLABORATION,ON_CALL,COVERAGE' AFTER doctor_id,
    ADD COLUMN is_primary_flag VARCHAR(8) NOT NULL DEFAULT 'Y' COMMENT '是否主责 Y/N' AFTER relationship_type,
    ADD COLUMN coverage_start_at DATETIME NULL COMMENT '覆盖开始' AFTER relationship_status,
    ADD COLUMN coverage_end_at DATETIME NULL COMMENT '覆盖结束' AFTER coverage_start_at,
    ADD COLUMN access_scope_json JSON NULL COMMENT '访问范围' AFTER coverage_end_at,
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER access_scope_json;

ALTER TABLE relationship_change_request
    ADD COLUMN request_type VARCHAR(32) NOT NULL DEFAULT 'CLAIM' COMMENT 'CLAIM,RELEASE,TRANSFER,COLLABORATE' AFTER doctor_id,
    ADD COLUMN from_doctor_id BIGINT NULL COMMENT '转出医生ID' AFTER request_type,
    ADD COLUMN to_doctor_id BIGINT NULL COMMENT '转入医生ID' AFTER from_doctor_id,
    ADD COLUMN handoff_summary VARCHAR(500) NULL COMMENT '交接摘要' AFTER request_reason,
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER approval_time;
