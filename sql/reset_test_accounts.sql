-- 重置测试账号脚本
-- 用途:修复测试账号的状态,确保可以正常登录

USE mental_health;

-- 1. 激活所有管理员账号
UPDATE user
SET status = 1
WHERE role = 'ADMIN' AND deleted = 0;

-- 2. 激活所有患者账号
UPDATE user
SET status = 1
WHERE role = 'PATIENT' AND deleted = 0;

-- 3. 激活指定的医生账号(如果你想让某个医生直接激活)
-- UPDATE user
-- SET status = 1
-- WHERE username = 'ooo' AND role = 'DOCTOR' AND deleted = 0;

-- 4. 查看当前所有账号状态
SELECT
    id,
    username,
    role,
    CASE
        WHEN status = 0 THEN '待审核'
        WHEN status = 1 THEN '已激活'
        WHEN status = 2 THEN '已禁用'
        ELSE '未知'
    END AS status_name,
    CASE
        WHEN deleted = 0 THEN '正常'
        WHEN deleted = 1 THEN '已删除'
    END AS deleted_name,
    create_time
FROM user
ORDER BY id;

-- 账号说明:
-- status: 0=待审核, 1=已激活, 2=已禁用
-- deleted: 0=正常, 1=已逻辑删除
-- role: ADMIN=管理员, DOCTOR=医生, PATIENT=患者
