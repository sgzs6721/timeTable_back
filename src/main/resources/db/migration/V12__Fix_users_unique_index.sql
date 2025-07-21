-- V12__Fix_users_unique_index.sql
-- 修复用户表的唯一索引，将id字段包含在唯一键中

-- 1. 删除现有的复合唯一索引
DROP INDEX idx_users_username_deleted_status ON users;

-- 2. 删除重复的索引（如果存在）
-- 删除单独的username索引，因为下面会重新创建
DROP INDEX IF EXISTS idx_users_username ON users;

-- 3. 创建新的复合唯一索引 (username, is_deleted, status, id)
-- 这样可以确保每个用户记录都是唯一的，即使软删除后重新创建同名用户
CREATE UNIQUE INDEX idx_users_username_deleted_status_id ON users (username, is_deleted, status, id);

-- 4. 重新创建常用的查询索引
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_username_deleted ON users (username, is_deleted);

-- 5. 保留现有的其他索引
-- idx_users_is_deleted 和 idx_users_nickname 保持不变 