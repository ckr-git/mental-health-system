-- ============================================================
-- Phase 1 Upgrade Slice 3: 候补转正 + 到诊管理
-- 预约状态机升级、候补offer、锁位、改约、签到
-- ============================================================

-- 1) 候补通知offer
CREATE TABLE waitlist_offer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    waitlist_id BIGINT NOT NULL COMMENT '候补记录ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    offered_slot_date DATE NOT NULL COMMENT '提供的时段日期',
    offered_slot_start TIME NOT NULL COMMENT '提供的时段开始',
    offered_slot_end TIME NOT NULL COMMENT '提供的时段结束',
    offer_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,ACCEPTED,DECLINED,EXPIRED,CANCELLED',
    priority_score INT NOT NULL DEFAULT 0 COMMENT '优先级评分',
    priority_factors_json JSON NULL COMMENT '优先级因子',
    offered_at DATETIME NOT NULL COMMENT '发出时间',
    expire_at DATETIME NOT NULL COMMENT '过期时间',
    responded_at DATETIME NULL COMMENT '响应时间',
    decline_reason VARCHAR(200) NULL COMMENT '拒绝原因',
    resulting_appointment_id BIGINT NULL COMMENT '转正后的预约ID',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_offer_waitlist (waitlist_id, offer_status),
    INDEX idx_offer_patient (patient_id, offer_status, expire_at),
    INDEX idx_offer_expiry (offer_status, expire_at),
    CONSTRAINT fk_offer_waitlist FOREIGN KEY (waitlist_id) REFERENCES appointment_waitlist(id),
    CONSTRAINT fk_offer_patient FOREIGN KEY (patient_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候补通知offer';

-- 2) 预约时段锁位
CREATE TABLE appointment_slot_hold (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    slot_date DATE NOT NULL COMMENT '时段日期',
    slot_start TIME NOT NULL COMMENT '时段开始',
    slot_end TIME NOT NULL COMMENT '时段结束',
    hold_status VARCHAR(32) NOT NULL DEFAULT 'HELD' COMMENT 'HELD,CONVERTED,RELEASED,EXPIRED',
    hold_source VARCHAR(32) NOT NULL COMMENT 'WAITLIST_OFFER,BOOKING_IN_PROGRESS',
    source_id BIGINT NULL COMMENT '来源ID',
    held_at DATETIME NOT NULL COMMENT '锁定时间',
    expire_at DATETIME NOT NULL COMMENT '过期时间',
    converted_at DATETIME NULL COMMENT '转正时间',
    resulting_appointment_id BIGINT NULL COMMENT '转正的预约ID',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_hold_doctor_slot (doctor_id, slot_date, slot_start, hold_status),
    INDEX idx_hold_expiry (hold_status, expire_at),
    CONSTRAINT fk_hold_doctor FOREIGN KEY (doctor_id) REFERENCES user(id),
    CONSTRAINT fk_hold_patient FOREIGN KEY (patient_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约时段锁位';

-- 3) 改约日志
CREATE TABLE appointment_reschedule_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    appointment_id BIGINT NOT NULL COMMENT '预约ID',
    original_time DATETIME NOT NULL COMMENT '原预约时间',
    new_time DATETIME NOT NULL COMMENT '新预约时间',
    reschedule_reason VARCHAR(200) NULL COMMENT '改约原因',
    rescheduled_by BIGINT NOT NULL COMMENT '改约人',
    rescheduled_by_role VARCHAR(16) NOT NULL COMMENT 'PATIENT,DOCTOR,ADMIN',
    rescheduled_from_id BIGINT NULL COMMENT '改约来源预约ID',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reschedule_appointment (appointment_id, create_time),
    CONSTRAINT fk_reschedule_appointment FOREIGN KEY (appointment_id) REFERENCES appointment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='改约日志';

-- 4) 扩展现有appointment表（使用存储过程避免列已存在错误）
DELIMITER //
DROP PROCEDURE IF EXISTS add_appointment_columns//
CREATE PROCEDURE add_appointment_columns()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment' AND column_name='attendance_status') THEN
        ALTER TABLE appointment ADD COLUMN attendance_status VARCHAR(32) NULL COMMENT 'PENDING,CHECKED_IN,COMPLETED,NO_SHOW,LATE_CANCEL' AFTER status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment' AND column_name='check_in_time') THEN
        ALTER TABLE appointment ADD COLUMN check_in_time DATETIME NULL COMMENT '签到时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment' AND column_name='cancelled_by') THEN
        ALTER TABLE appointment ADD COLUMN cancelled_by BIGINT NULL COMMENT '取消人';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment' AND column_name='rescheduled_from_id') THEN
        ALTER TABLE appointment ADD COLUMN rescheduled_from_id BIGINT NULL COMMENT '改约来源预约ID';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment' AND column_name='priority_code') THEN
        ALTER TABLE appointment ADD COLUMN priority_code VARCHAR(16) NULL COMMENT 'NORMAL,URGENT,FOLLOWUP,CRISIS';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment' AND column_name='version') THEN
        ALTER TABLE appointment ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本';
    END IF;
END//
DELIMITER ;
CALL add_appointment_columns();
DROP PROCEDURE IF EXISTS add_appointment_columns;

-- 5) 扩展现有appointment_waitlist表
DELIMITER //
DROP PROCEDURE IF EXISTS add_waitlist_columns//
CREATE PROCEDURE add_waitlist_columns()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment_waitlist' AND column_name='priority_score') THEN
        ALTER TABLE appointment_waitlist ADD COLUMN priority_score INT NOT NULL DEFAULT 0 COMMENT '优先级评分' AFTER symptoms;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment_waitlist' AND column_name='priority_factors_json') THEN
        ALTER TABLE appointment_waitlist ADD COLUMN priority_factors_json JSON NULL COMMENT '优先级因子';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment_waitlist' AND column_name='risk_level') THEN
        ALTER TABLE appointment_waitlist ADD COLUMN risk_level VARCHAR(16) NULL COMMENT '患者风险等级';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment_waitlist' AND column_name='is_followup') THEN
        ALTER TABLE appointment_waitlist ADD COLUMN is_followup VARCHAR(8) NOT NULL DEFAULT 'N' COMMENT '是否复诊';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment_waitlist' AND column_name='treatment_plan_id') THEN
        ALTER TABLE appointment_waitlist ADD COLUMN treatment_plan_id BIGINT NULL COMMENT '关联治疗计划';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment_waitlist' AND column_name='offer_expire_at') THEN
        ALTER TABLE appointment_waitlist ADD COLUMN offer_expire_at DATETIME NULL COMMENT 'Offer过期时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment_waitlist' AND column_name='accepted_at') THEN
        ALTER TABLE appointment_waitlist ADD COLUMN accepted_at DATETIME NULL COMMENT '接受时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment_waitlist' AND column_name='decline_reason') THEN
        ALTER TABLE appointment_waitlist ADD COLUMN decline_reason VARCHAR(200) NULL COMMENT '拒绝原因';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='appointment_waitlist' AND column_name='version') THEN
        ALTER TABLE appointment_waitlist ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本';
    END IF;
END//
DELIMITER ;
CALL add_waitlist_columns();
DROP PROCEDURE IF EXISTS add_waitlist_columns;
