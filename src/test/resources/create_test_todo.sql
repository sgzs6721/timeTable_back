-- ============================================
-- 微信待办推送测试脚本
-- ============================================

-- 步骤1：查询已绑定微信的用户
-- 请先运行这条SQL，找到一个有 wechat_openid 的用户
SELECT 
    id AS user_id,
    username,
    nickname,
    wechat_openid,
    organization_id
FROM users 
WHERE deleted = 0 
  AND wechat_openid IS NOT NULL 
LIMIT 5;

-- ============================================
-- 步骤2：创建测试待办
-- 请将下面的 @user_id 和 @org_id 替换为步骤1查询到的实际值
-- ============================================

SET @user_id = 1;        -- 替换为实际的用户ID
SET @org_id = 1;         -- 替换为实际的机构ID

-- 创建一条立即需要推送的测试待办
INSERT INTO todos (
    customer_name,           -- 客户姓名
    content,                 -- 待办内容
    reminder_date,           -- 提醒日期（今天）
    reminder_time,           -- 提醒时间（当前时间）
    type,                    -- 待办类型
    status,                  -- 待办状态
    is_read,                 -- 是否已读
    created_by,              -- 创建人
    organization_id,         -- 机构ID
    push_status,             -- 推送状态：PENDING-待推送
    push_retry_count,        -- 重试次数
    created_at,
    updated_at,
    deleted
) VALUES (
    '测试客户-张三',                    -- 客户姓名
    '跟进意向客户',                     -- 待办内容
    CURDATE(),                          -- 今天
    CURTIME(),                          -- 当前时间
    'CUSTOMER_FOLLOW_UP',               -- 类型：客户跟进
    'PENDING',                          -- 状态：待处理
    0,                                  -- 未读
    @user_id,                           -- 使用上面设置的用户ID
    @org_id,                            -- 使用上面设置的机构ID
    'PENDING',                          -- 待推送
    0,                                  -- 重试次数0
    NOW(),
    NOW(),
    0
);

-- 获取刚创建的待办ID
SELECT LAST_INSERT_ID() AS 'new_todo_id';

-- ============================================
-- 步骤3：查看创建的测试待办
-- ============================================
SELECT 
    id,
    customer_name,
    content,
    reminder_date,
    reminder_time,
    push_status,
    pushed_at,
    push_retry_count,
    push_error_message,
    created_by
FROM todos 
WHERE deleted = 0 
  AND created_by = @user_id
ORDER BY created_at DESC 
LIMIT 5;

-- ============================================
-- 步骤4：验证用户是否已绑定微信
-- ============================================
SELECT 
    id,
    username,
    nickname,
    wechat_openid,
    CASE 
        WHEN wechat_openid IS NOT NULL THEN '✅ 已绑定微信'
        ELSE '❌ 未绑定微信，无法推送'
    END AS wechat_status
FROM users 
WHERE id = @user_id;

-- ============================================
-- 后续查询：查看推送结果（等待定时任务执行后运行）
-- ============================================

-- 查看所有待办的推送状态
SELECT 
    id,
    customer_name,
    content,
    reminder_date,
    reminder_time,
    push_status,
    CASE 
        WHEN push_status = 'PENDING' THEN '⏳ 待推送'
        WHEN push_status = 'PUSHED' THEN '✅ 已推送'
        WHEN push_status = 'FAILED' THEN '❌ 推送失败'
        ELSE push_status
    END AS push_status_text,
    pushed_at,
    push_retry_count,
    push_error_message,
    created_at
FROM todos 
WHERE deleted = 0 
  AND push_status IN ('PENDING', 'PUSHED', 'FAILED')
ORDER BY created_at DESC 
LIMIT 10;

-- ============================================
-- 清理测试数据（可选）
-- ============================================

-- 如果需要删除测试待办，运行以下SQL
-- DELETE FROM todos WHERE customer_name LIKE '测试客户-%';

-- 或者软删除
-- UPDATE todos SET deleted = 1 WHERE customer_name LIKE '测试客户-%';

