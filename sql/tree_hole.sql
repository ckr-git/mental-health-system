-- 心情树洞表
CREATE TABLE IF NOT EXISTS tree_hole (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',

    -- 倾诉对象
    speak_to_type VARCHAR(20) NOT NULL COMMENT '对象类型: self-自己, person-某人, role-角色, thing-某物, custom-自定义',
    speak_to_name VARCHAR(100) NOT NULL COMMENT '对象名称',

    -- 内容
    content TEXT NOT NULL COMMENT '倾诉内容',
    emotion_tags VARCHAR(200) COMMENT '情绪标签（JSON数组）',
    content_type VARCHAR(20) DEFAULT 'text' COMMENT '内容类型: text-文字, voice-语音, drawing-涂鸦',
    attachment_url VARCHAR(255) COMMENT '附件URL（语音或涂鸦）',

    -- 时间设置
    expire_type VARCHAR(20) NOT NULL COMMENT '消失类型: 5sec, 1hour, tonight, tomorrow, forever, conditional',
    expire_time DATETIME COMMENT '消失时间',
    is_expired TINYINT DEFAULT 0 COMMENT '是否已消失: 0-未消失, 1-已消失',

    -- 查看条件（仅conditional类型）
    view_condition VARCHAR(100) COMMENT '查看条件: mood_low<3, mood_high>8, after_30days',
    can_view TINYINT DEFAULT 0 COMMENT '当前是否可查看: 0-不可查看, 1-可查看',

    -- 状态
    mood_at_write INT COMMENT '写入时心情评分',
    view_count INT DEFAULT 0 COMMENT '查看次数',
    last_view_time DATETIME COMMENT '最后查看时间',

    -- 时间戳
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_expire_time (expire_time),
    INDEX idx_is_expired (is_expired),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='心情树洞表';
