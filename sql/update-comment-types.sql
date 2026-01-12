-- 更新 mood_comment 表的 comment_type 字段定义
USE mental_health;

ALTER TABLE mood_comment 
MODIFY COLUMN comment_type VARCHAR(20) DEFAULT 'agree' 
COMMENT '类型：agree-赞同, disagree-不赞同, heartache-心疼, encourage-鼓励, relief-释然';

-- 查看修改结果
DESCRIBE mood_comment;
