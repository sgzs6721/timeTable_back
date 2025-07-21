-- V12__Fix_users_unique_index.sql
-- 修复用户表的唯一索引，将id字段包含在唯一键中

-- 2. 删除重复的索引（如果存在）
-- 由于MySQL不支持DROP INDEX IF EXISTS，我们需要手动处理
-- 如果索引存在，则删除它
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'users'
     AND INDEX_NAME = 'idx_users_username'
     AND NON_UNIQUE = 1) > 0,
    'ALTER TABLE users DROP INDEX idx_users_username',
    'SELECT "Username index does not exist"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 创建新的复合唯一索引 (username, is_deleted, status, id)
-- 这样可以确保每个用户记录都是唯一的，即使软删除后重新创建同名用户
CREATE UNIQUE INDEX idx_users_username_deleted_status_id ON users (username, is_deleted, status, id);

-- 4. 重新创建常用的查询索引
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_username_deleted ON users (username, is_deleted);

-- 5. 保留现有的其他索引
-- idx_users_is_deleted 和 idx_users_nickname 保持不变
