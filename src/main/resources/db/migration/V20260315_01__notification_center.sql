-- ============================================================
-- Slice 1: 通知中心基础设施
-- ============================================================

CREATE TABLE user_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(32) NOT NULL COMMENT 'CRISIS,APPOINTMENT,ASSESSMENT,SYSTEM',
    priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'LOW,NORMAL,HIGH,URGENT',
    title VARCHAR(200) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    action_type VARCHAR(32) NULL COMMENT 'ROUTE,API,EXTERNAL',
    action_payload JSON NULL,
    source_type VARCHAR(32) NULL,
    source_id BIGINT NULL,
    read_status TINYINT NOT NULL DEFAULT 0,
    read_time DATETIME NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notif_user_read_time (user_id, read_status, create_time),
    INDEX idx_notif_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知收件箱';
