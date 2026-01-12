-- Phase 3: 主题解锁系统
-- 更新 user_theme_config 表，添加主题解锁相关字段

-- 添加当前主题字段
ALTER TABLE user_theme_config
ADD COLUMN current_theme VARCHAR(50) DEFAULT 'default_day' COMMENT '当前主题' AFTER user_id;

-- 添加已解锁主题列表（JSON）
ALTER TABLE user_theme_config
ADD COLUMN unlocked_themes JSON COMMENT '已解锁的主题列表' AFTER light_mode;

-- 添加夜晚模式使用次数
ALTER TABLE user_theme_config
ADD COLUMN night_mode_usage_count INT DEFAULT 0 COMMENT '夜晚模式使用次数' AFTER light_toggle_count;

-- 添加连续打卡天数
ALTER TABLE user_theme_config
ADD COLUMN consecutive_check_in_days INT DEFAULT 0 COMMENT '连续打卡天数' AFTER total_letter_count;

-- 添加度过低谷次数
ALTER TABLE user_theme_config
ADD COLUMN low_mood_survived_count INT DEFAULT 0 COMMENT '度过的低谷次数' AFTER consecutive_check_in_days;

-- 为现有用户初始化主题数据
UPDATE user_theme_config
SET current_theme = 'default_day',
    unlocked_themes = JSON_ARRAY('default_day'),
    night_mode_usage_count = 0,
    consecutive_check_in_days = 0,
    low_mood_survived_count = 0
WHERE current_theme IS NULL;

-- 主题解锁条件说明:
-- 1. default_day: 默认主题，所有用户自动拥有
-- 2. christmas: 圣诞主题 - 12月自动解锁
-- 3. newyear: 新年主题 - 1月自动解锁
-- 4. halloween: 万圣节主题 - 10月自动解锁
-- 5. cherry_blossom: 樱花主题 - 连续打卡30天
-- 6. seaside: 海边小屋 - 完成10次时光信箱
-- 7. mountain: 山间木屋 - 度过5次低谷
-- 8. starry: 星空露营 - 夜晚模式使用30次
