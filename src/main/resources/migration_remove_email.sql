-- Migration脚本：移除users表的email字段
-- 执行时间：2025-07-01
-- 目的：简化用户注册流程，移除email字段

-- 1. 删除email字段的索引（如果存在）
DROP INDEX IF EXISTS idx_users_email;

-- 2. 删除email字段
ALTER TABLE users DROP COLUMN IF EXISTS email;

-- 3. 验证表结构
-- SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'USERS'; 