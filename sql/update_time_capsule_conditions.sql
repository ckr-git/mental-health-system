-- 为时光信箱添加特殊条件字段
ALTER TABLE time_capsule
ADD COLUMN unlock_type VARCHAR(20) DEFAULT 'days' COMMENT '解锁类型：days-指定天数, date-指定日期, condition-特殊条件',
ADD COLUMN unlock_conditions VARCHAR(200) COMMENT '解锁条件（JSON数组）：mood_low, mood_high, days_30';

-- 更新现有数据，默认为days类型
UPDATE time_capsule SET unlock_type = 'days' WHERE unlock_type IS NULL;

SELECT '✅ 时光信箱表结构更新完成！' as status;
