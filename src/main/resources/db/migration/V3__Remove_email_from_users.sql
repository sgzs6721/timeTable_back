-- V3__Remove_email_from_users.sql
-- 移除users表的email字段及相关索引

-- 1. 删除email字段索引（如果存在，若不存在可忽略报错）
ALTER TABLE users DROP INDEX idx_users_email;

-- 2. 删除email字段（如果存在）
ALTER TABLE users DROP COLUMN email; 