-- 房间装饰物表
CREATE TABLE IF NOT EXISTS room_decoration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '装饰物ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',

    -- 装饰物信息
    decoration_type VARCHAR(30) NOT NULL COMMENT '装饰物类型：plant-绿植, photo_frame-相框, bookshelf-书架, candle-蜡烛, music_box-音乐盒, etc',
    decoration_name VARCHAR(100) COMMENT '装饰物名称',
    decoration_icon VARCHAR(10) COMMENT '装饰物图标emoji',

    -- 位置信息
    position_x INT COMMENT 'X坐标（百分比 0-100）',
    position_y INT COMMENT 'Y坐标（百分比 0-100）',
    position_z INT DEFAULT 0 COMMENT 'Z-index层级',
    scale DECIMAL(3,2) DEFAULT 1.00 COMMENT '缩放比例 0.5-2.0',
    rotation INT DEFAULT 0 COMMENT '旋转角度 0-360',

    -- 状态信息
    is_unlocked TINYINT DEFAULT 0 COMMENT '是否已解锁',
    unlock_condition VARCHAR(100) COMMENT '解锁条件：diary_count_30, mood_streak_7, etc',
    is_active TINYINT DEFAULT 1 COMMENT '是否激活显示',

    -- 互动数据
    interaction_count INT DEFAULT 0 COMMENT '互动次数',
    last_interaction_time DATETIME COMMENT '最后互动时间',
    custom_data JSON COMMENT '自定义数据（如相框照片URL、音乐盒音乐URL等）',

    -- 时间戳
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user (user_id),
    INDEX idx_type (decoration_type),
    INDEX idx_unlock (is_unlocked)
) COMMENT='房间装饰物表';

-- 装饰物配置表（预定义的装饰物类型）
CREATE TABLE IF NOT EXISTS decoration_config (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',

    -- 装饰物基本信息
    decoration_type VARCHAR(30) NOT NULL UNIQUE COMMENT '装饰物类型',
    decoration_name VARCHAR(100) NOT NULL COMMENT '装饰物名称',
    decoration_icon VARCHAR(10) NOT NULL COMMENT '装饰物图标',
    category VARCHAR(30) COMMENT '分类：furniture-家具, plant-植物, decoration-装饰, special-特殊',

    -- 解锁条件
    unlock_condition VARCHAR(100) COMMENT '解锁条件描述',
    unlock_requirement JSON COMMENT '解锁要求（JSON）',

    -- 互动配置
    can_interact TINYINT DEFAULT 0 COMMENT '是否可互动',
    interaction_type VARCHAR(30) COMMENT '互动类型：water-浇水, play_music-播放音乐, view-查看',
    interaction_effect VARCHAR(200) COMMENT '互动效果描述',

    -- 视觉配置
    default_scale DECIMAL(3,2) DEFAULT 1.00 COMMENT '默认缩放',
    size_class VARCHAR(20) DEFAULT 'medium' COMMENT '大小分类：small, medium, large',
    animation_config JSON COMMENT '动画配置',

    -- 顺序和推荐
    display_order INT DEFAULT 0 COMMENT '显示顺序',
    is_recommended TINYINT DEFAULT 0 COMMENT '是否推荐',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='装饰物配置表';

-- 插入预定义装饰物配置
INSERT INTO decoration_config (decoration_type, decoration_name, decoration_icon, category, unlock_condition, unlock_requirement, can_interact, interaction_type, interaction_effect, size_class, display_order, is_recommended) VALUES
-- 绿植类
('plant_cactus', '小仙人掌', '🌵', 'plant', '初始解锁', '{}', 1, 'water', '浇水后会长大一点点', 'small', 1, 1),
('plant_pothos', '绿萝', '🪴', 'plant', '记录7天日记', '{"diary_count": 7}', 1, 'water', '浇水后叶子会变得更绿', 'medium', 2, 1),
('plant_monstera', '龟背竹', '🌿', 'plant', '记录30天日记', '{"diary_count": 30}', 1, 'water', '浇水后会长出新叶', 'large', 3, 0),

-- 相框类
('frame_photo', '相框', '🖼️', 'decoration', '初始解锁', '{}', 1, 'view', '点击可放大查看照片', 'medium', 4, 1),
('frame_painting', '画框', '🎨', 'decoration', '心情好转5次', '{"mood_better_count": 5}', 1, 'view', '点击欣赏艺术作品', 'medium', 5, 0),

-- 家具类
('bookshelf', '书架', '📚', 'furniture', '写10封信', '{"letter_count": 10}', 1, 'view', '点击可阅读书籍', 'large', 6, 0),
('desk_lamp', '台灯', '💡', 'furniture', '开关灯30次', '{"light_toggle_count": 30}', 1, 'toggle', '点击可开关台灯', 'small', 7, 1),

-- 装饰物类
('candle', '蜡烛', '🕯️', 'decoration', '连续7天记录', '{"diary_streak": 7}', 1, 'light', '点击可点燃蜡烛', 'small', 8, 1),
('music_box', '音乐盒', '🎵', 'decoration', '心情>8分连续3天', '{"high_mood_streak": 3}', 1, 'play_music', '点击播放治愈音乐', 'small', 9, 1),
('star_light', '星星灯', '⭐', 'decoration', '战胜困难5次', '{"overcome_count": 5}', 1, 'toggle', '点击星星会闪烁', 'small', 10, 0),

-- 特殊装饰
('pet_cat', '小猫咪', '🐱', 'special', '记录100天日记', '{"diary_count": 100}', 1, 'pet', '点击可抚摸小猫', 'medium', 11, 0),
('dream_catcher', '捕梦网', '🌙', 'special', '记录10个梦境', '{"dream_count": 10}', 1, 'view', '点击查看捕获的梦', 'medium', 12, 0),
('lucky_charm', '幸运符', '🍀', 'special', '度过低谷10次', '{"low_mood_overcome": 10}', 1, 'blessing', '点击获得祝福', 'small', 13, 0);
