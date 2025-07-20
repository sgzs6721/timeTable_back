-- V11__Modify_user_unique_identifier.sql
-- 修改用户唯一标识，使用 (username, is_deleted, status) 作为复合唯一标识

-- 1. 移除用户名的唯一约束
-- 由于MySQL不支持DROP INDEX IF EXISTS，我们需要手动处理
-- 如果唯一约束存在，则删除它
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'users' 
     AND INDEX_NAME = 'username' 
     AND NON_UNIQUE = 0) > 0,
    'ALTER TABLE users DROP INDEX username',
    'SELECT "Username unique constraint does not exist"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 创建复合唯一索引 (username, is_deleted, status)
-- 这样可以确保同一用户名在相同的删除状态和用户状态下是唯一的
CREATE UNIQUE INDEX idx_users_username_deleted_status ON users (username, is_deleted, status);

-- 3. 为复合索引的各个字段创建单独的索引以提高查询性能
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_is_deleted ON users (is_deleted);
CREATE INDEX idx_users_status ON users (status);

-- 4. 创建复合索引 (username, is_deleted) 用于常见的查询场景
CREATE INDEX idx_users_username_deleted ON users (username, is_deleted); 