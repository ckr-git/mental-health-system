-- =============================================
-- 情绪日记系统 - 第一阶段数据库脚本
-- =============================================

USE mental_health;

-- =============================================
-- 1. 备份旧表（如果存在）
-- =============================================
-- 如果已经备份过，跳过此步骤
DROP TABLE IF EXISTS symptom_record_backup;
RENAME TABLE IF EXISTS symptom_record TO symptom_record_backup;

-- =============================================
-- 2. 情绪日记表（核心表）
-- =============================================
CREATE TABLE mood_diary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- 基础信息
    mood_score INT NOT NULL COMMENT '心情评分 1-10',
    mood_emoji VARCHAR(10) COMMENT '心情表情 😢😔😐🙂😊😄🥰',
    title VARCHAR(100) COMMENT '标题（一句话描述）',
    content TEXT COMMENT '详细内容',
    
    -- 多维度评分
    energy_level INT COMMENT '精力水平 1-10',
    sleep_quality INT COMMENT '睡眠质量 1-10',
    stress_level INT COMMENT '压力水平 1-10',
    
    -- 天气主题
    weather_type VARCHAR(20) COMMENT '天气类型：storm, rain, cloudy, sunny, clear',
    weather_config JSON COMMENT '天气配置（颜色、粒子参数）',
    
    -- 状态标记
    status VARCHAR(20) DEFAULT 'ongoing' COMMENT '状态：ongoing-正在经历, better-已好转, overcome-已度过, proud-我战胜了它',
    status_update_time DATETIME COMMENT '状态更新时间',
    
    -- 统计数据
    view_count INT DEFAULT 0 COMMENT '回看次数',
    comment_count INT DEFAULT 0 COMMENT '留言数',
    interaction_count JSON COMMENT '互动统计 {"agree":3, "heartache":5, ...}',
    
    -- 时间戳
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_time (user_id, create_time),
    INDEX idx_user_status (user_id, status),
    INDEX idx_weather (weather_type),
    
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='情绪日记表';

-- =============================================
-- 3. 心情留言表
-- =============================================
CREATE TABLE mood_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    diary_id BIGINT NOT NULL COMMENT '日记ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- 留言内容
    content TEXT NOT NULL COMMENT '留言内容',
    comment_type VARCHAR(20) DEFAULT 'random' COMMENT '类型：random-随便说说, praise-点赞, hug-抱抱, note-想说, thought-现在想法',
    
    -- 互动标记（多选，JSON数组）
    interactions JSON COMMENT '互动类型 ["agree", "heartache", "encourage"]',
    
    -- 情绪对比
    mood_at_comment INT COMMENT '留言时的心情',
    mood_at_diary INT COMMENT '日记时的心情',
    mood_gap INT COMMENT '心情差值（自动计算）',
    
    -- 时间戳
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_diary (diary_id),
    INDEX idx_user (user_id),
    INDEX idx_create_time (create_time),
    
    FOREIGN KEY (diary_id) REFERENCES mood_diary(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='心情留言表';

-- =============================================
-- 4. 时光信箱表
-- =============================================
CREATE TABLE time_capsule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- 信件信息
    letter_type VARCHAR(30) NOT NULL COMMENT '信件类型：praise-表扬信, hope-期望信, thanks-感谢信',
    title VARCHAR(100) COMMENT '信件标题',
    content TEXT NOT NULL COMMENT '信件内容',
    
    -- 触发数据
    trigger_mood_avg DECIMAL(3,1) COMMENT '触发时平均心情',
    trigger_mood_trend VARCHAR(20) COMMENT '触发趋势：up-上升, stable-平稳, down-下降',
    related_diary_ids JSON COMMENT '关联日记ID列表',
    
    -- 时间控制
    write_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '写信时间',
    unlock_date DATE NOT NULL COMMENT '解锁日期',
    unlock_days INT COMMENT '解锁天数（方便查询）',
    
    -- 状态
    status VARCHAR(20) DEFAULT 'sealed' COMMENT '状态：sealed-封存, unlocked-已解锁, read-已读, replied-已回复',
    unlock_time DATETIME COMMENT '解锁时间',
    read_time DATETIME COMMENT '阅读时间',
    reply_content TEXT COMMENT '回复内容',
    reply_time DATETIME COMMENT '回复时间',
    
    -- 时间戳
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_user (user_id),
    INDEX idx_unlock_date (unlock_date, status),
    INDEX idx_status (status),
    
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='时光信箱表';

-- =============================================
-- 5. 用户主题配置表
-- =============================================
CREATE TABLE user_theme_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    
    -- 灯光模式
    light_mode VARCHAR(10) DEFAULT 'day' COMMENT '灯光模式：day-白天, night-夜晚',
    
    -- 天气效果开关
    weather_enabled TINYINT DEFAULT 1 COMMENT '天气效果开关 0-关闭 1-开启',
    particle_enabled TINYINT DEFAULT 1 COMMENT '粒子效果开关 0-关闭 1-开启',
    animation_enabled TINYINT DEFAULT 1 COMMENT '动画效果开关 0-关闭 1-开启',
    
    -- 音效开关
    sound_enabled TINYINT DEFAULT 1 COMMENT '音效开关 0-关闭 1-开启',
    volume INT DEFAULT 50 COMMENT '音量 0-100',
    
    -- 统计数据
    light_toggle_count INT DEFAULT 0 COMMENT '开关灯次数',
    total_diary_count INT DEFAULT 0 COMMENT '总日记数',
    total_comment_count INT DEFAULT 0 COMMENT '总留言数',
    total_letter_count INT DEFAULT 0 COMMENT '总信件数',
    
    -- 时间戳
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user (user_id),
    
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户主题配置表';

-- =============================================
-- 6. 天气主题配置表（系统配置表）
-- =============================================
CREATE TABLE weather_config (
    id INT PRIMARY KEY AUTO_INCREMENT,
    weather_type VARCHAR(20) UNIQUE NOT NULL COMMENT '天气类型',
    weather_name VARCHAR(50) NOT NULL COMMENT '天气名称',
    
    -- 心情范围
    mood_min INT NOT NULL COMMENT '心情最小值 1-10',
    mood_max INT NOT NULL COMMENT '心情最大值 1-10',
    
    -- 视觉配置
    bg_gradient_start VARCHAR(10) NOT NULL COMMENT '背景渐变起始色 #RRGGBB',
    bg_gradient_end VARCHAR(10) NOT NULL COMMENT '背景渐变结束色 #RRGGBB',
    weather_icon VARCHAR(20) COMMENT '天气图标 emoji',
    
    -- 粒子配置
    particle_type VARCHAR(30) COMMENT '粒子类型：rain, snow, cloud, sunshine',
    particle_color VARCHAR(10) COMMENT '粒子颜色 #RRGGBB',
    particle_count INT DEFAULT 50 COMMENT '粒子数量',
    particle_speed DECIMAL(3,2) DEFAULT 1.0 COMMENT '粒子速度',
    
    -- 音效
    ambient_sound VARCHAR(100) COMMENT '环境音文件名',
    
    -- 描述
    description VARCHAR(200) COMMENT '天气描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='天气主题配置表';

-- =============================================
-- 7. 插入天气配置初始数据
-- =============================================
INSERT INTO weather_config (weather_type, weather_name, mood_min, mood_max, bg_gradient_start, bg_gradient_end, weather_icon, particle_type, particle_color, particle_count, particle_speed, ambient_sound, description) VALUES
('storm', '暴风雨', 1, 2, '#1a1a2e', '#16213e', '🌩️', 'rain', '#6B8CAE', 80, 2.5, 'storm.mp3', '心情很差，像暴风雨一样狂躁不安'),
('rain', '阴雨绵绵', 3, 4, '#4A5FC7', '#6B7FD7', '🌧️', 'rain', '#8BA4D9', 60, 1.5, 'rain.mp3', '有些低落，阴雨绵绵挥之不去'),
('cloudy', '多云', 5, 6, '#8BA4D9', '#B8C5E0', '☁️', 'cloud', '#D4DCF0', 40, 0.8, 'wind.mp3', '平和的心情，像多云的天空'),
('sunny', '晴朗', 7, 8, '#FFD700', '#FFA500', '🌤️', 'sunshine', '#FFEB99', 30, 0.5, 'birds.mp3', '心情不错，阳光明媚温暖'),
('clear', '晴空万里', 9, 10, '#87CEEB', '#00BFFF', '☀️', 'sunshine', '#FFEB3B', 50, 0.3, 'happy.mp3', '心情极好，万里晴空心旷神怡');

-- =============================================
-- 8. 初始化触发器 - 自动更新留言数
-- =============================================
DELIMITER //

CREATE TRIGGER update_comment_count_after_insert
AFTER INSERT ON mood_comment
FOR EACH ROW
BEGIN
    UPDATE mood_diary 
    SET comment_count = comment_count + 1 
    WHERE id = NEW.diary_id;
END//

CREATE TRIGGER update_comment_count_after_delete
AFTER DELETE ON mood_comment
FOR EACH ROW
BEGIN
    UPDATE mood_diary 
    SET comment_count = comment_count - 1 
    WHERE id = OLD.diary_id AND comment_count > 0;
END//

DELIMITER ;

-- =============================================
-- 9. 创建视图 - 用户情绪统计
-- =============================================
CREATE VIEW user_mood_stats AS
SELECT 
    user_id,
    COUNT(*) as total_diaries,
    AVG(mood_score) as avg_mood,
    MAX(mood_score) as max_mood,
    MIN(mood_score) as min_mood,
    SUM(CASE WHEN status = 'proud' THEN 1 ELSE 0 END) as overcome_count,
    SUM(view_count) as total_views,
    SUM(comment_count) as total_comments
FROM mood_diary
GROUP BY user_id;

-- =============================================
-- 10. 验证表创建
-- =============================================
SELECT 
    '✅ 数据库表创建完成！' as message,
    COUNT(*) as table_count
FROM information_schema.tables 
WHERE table_schema = 'mental_health' 
AND table_name IN ('mood_diary', 'mood_comment', 'time_capsule', 'user_theme_config', 'weather_config');

-- 显示天气配置
SELECT '✅ 天气配置数据：' as message;
SELECT weather_type, weather_name, mood_min, mood_max, weather_icon FROM weather_config ORDER BY mood_min;

SELECT '✅ 第一阶段数据库准备完成！可以开始开发了 🚀' as status;
