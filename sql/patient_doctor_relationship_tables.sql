-- 医患关系管理系统数据库表创建脚本
-- 执行方式: mysql -uroot -p你的密码 mental_health < patient_doctor_relationship_tables.sql

USE mental_health;

-- 1. 医患关系表
CREATE TABLE IF NOT EXISTS patient_doctor_relationship (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    relationship_status VARCHAR(20) DEFAULT 'active' COMMENT '关系状态: active-激活, inactive-停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_patient_active (patient_id, deleted, relationship_status) COMMENT '患者只能有一个激活关系',
    INDEX idx_doctor (doctor_id),
    INDEX idx_status (relationship_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医患关系表';

-- 2. 患者分配变更请求表（认领/释放需要审核）
CREATE TABLE IF NOT EXISTS relationship_change_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型: claim-认领, release-释放',
    request_status VARCHAR(20) DEFAULT 'pending' COMMENT '请求状态: pending-待审核, approved-通过, rejected-拒绝',
    request_reason TEXT COMMENT '申请理由',
    admin_id BIGINT COMMENT '审核管理员ID',
    admin_note TEXT COMMENT '审核备注',
    approval_time DATETIME COMMENT '审核时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_status (request_status),
    INDEX idx_doctor (doctor_id),
    INDEX idx_patient (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者分配变更请求表';

-- 3. 在线咨询会话表
CREATE TABLE IF NOT EXISTS consultation_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    session_status VARCHAR(20) DEFAULT 'ongoing' COMMENT '会话状态: ongoing-进行中, closed-已关闭',
    start_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_patient (patient_id),
    INDEX idx_doctor (doctor_id),
    INDEX idx_status (session_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线咨询会话表';

-- 初始化测试数据：建立医生ooo(id=7)和患者1(id=9)的关系
INSERT INTO patient_doctor_relationship (patient_id, doctor_id, relationship_status)
VALUES (9, 7, 'active')
ON DUPLICATE KEY UPDATE relationship_status='active';

SELECT '数据库表创建完成！' AS message;
