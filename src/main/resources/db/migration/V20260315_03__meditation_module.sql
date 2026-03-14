-- ============================================================
-- Slice 5: 冥想/呼吸练习模块
-- ============================================================

CREATE TABLE meditation_exercise (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exercise_code VARCHAR(64) NOT NULL,
    title VARCHAR(100) NOT NULL,
    category VARCHAR(32) NOT NULL COMMENT 'BREATHING,BODY_SCAN,PMR,GROUNDING',
    description TEXT NULL,
    duration_seconds INT NOT NULL,
    inhale_seconds INT NULL,
    hold_seconds INT NULL,
    exhale_seconds INT NULL,
    rest_seconds INT NULL,
    instruction_steps JSON NOT NULL,
    animation_preset VARCHAR(32) NOT NULL DEFAULT 'CIRCLE',
    active TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_exercise_code (exercise_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冥想练习';

CREATE TABLE meditation_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    session_status VARCHAR(16) NOT NULL DEFAULT 'STARTED' COMMENT 'STARTED,COMPLETED,ABANDONED',
    started_at DATETIME NOT NULL,
    completed_at DATETIME NULL,
    planned_seconds INT NOT NULL,
    actual_seconds INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_med_user_time (user_id, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冥想会话记录';

-- 种子数据
INSERT INTO meditation_exercise (exercise_code, title, category, description, duration_seconds, inhale_seconds, hold_seconds, exhale_seconds, rest_seconds, instruction_steps, animation_preset) VALUES
('BOX_BREATHING', '方块呼吸 4-4-4-4', 'BREATHING', '均匀的呼吸节奏有助于激活副交感神经，快速平静心绪', 300, 4, 4, 4, 4, '["找一个舒适的坐姿","闭上眼睛，放松肩膀","用鼻子缓慢吸气4秒","屏住呼吸4秒","缓慢呼气4秒","保持空肺4秒","重复循环"]', 'CIRCLE'),
('CALMING_478', '安神呼吸 4-7-8', 'BREATHING', '经典的放松呼吸法，适合睡前或焦虑时使用', 360, 4, 7, 8, 0, '["舌尖轻抵上颚","完全呼气发出呼声","用鼻子吸气4秒","屏住呼吸7秒","用嘴呼气8秒发出呼声","重复4次为一组"]', 'CIRCLE'),
('COHERENT', '连贯呼吸', 'BREATHING', '每分钟约5-6次呼吸，促进心率变异性平衡', 600, 5, 0, 5, 0, '["自然坐姿，双手放在膝盖上","缓慢吸气5秒","缓慢呼气5秒","保持均匀节奏","感受腹部起伏"]', 'WAVE'),
('BODY_SCAN_5MIN', '5分钟身体扫描', 'BODY_SCAN', '从头到脚逐步放松每个身体部位', 300, 0, 0, 0, 0, '["闭上眼睛，做几次深呼吸","将注意力集中在头顶","感受面部肌肉，慢慢放松","注意肩膀和颈部的紧张感","感受双手和手臂","关注腹部的呼吸","放松双腿和双脚","感受整个身体的放松"]', 'BODY'),
('PMR_SHOULDERS', '渐进式肌肉放松 — 肩颈', 'PMR', '通过有意识地紧张和放松肌肉来减轻压力', 480, 0, 0, 0, 0, '["坐直，双脚平放地面","深呼吸三次","将双肩提起到耳朵位置，保持5秒","突然放松，感受差异","重复3次","转动头部，左右各3次","低头，感受颈后拉伸","抬头，回到中立位置","深呼吸，感受放松"]', 'BODY'),
('SLEEP_WIND_DOWN', '睡前放松', 'BREATHING', '帮助从日间活动过渡到安静睡眠状态', 720, 4, 7, 8, 2, '["躺下或靠在舒适的位置","闭上眼睛","进行3轮4-7-8呼吸","想象温暖的光从头顶流下","光经过面部，放松下巴","光流过肩膀和手臂","光温暖腹部和腰部","光到达双腿和脚底","感受全身被温暖包围"]', 'CIRCLE'),
('GROUNDING_54321', '5-4-3-2-1 落地法', 'GROUNDING', '通过五感将注意力拉回当下，适合恐慌发作时使用', 240, 0, 0, 0, 0, '["深呼吸三次","说出你看到的5样东西","说出你能触摸到的4样东西","说出你听到的3种声音","说出你闻到的2种气味","说出你尝到的1种味道","再做三次深呼吸"]', 'PULSE');
