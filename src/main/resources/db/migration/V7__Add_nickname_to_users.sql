-- V7__Add_nickname_to_users.sql
-- 为用户表添加昵称字段

-- 为用户表添加昵称字段
ALTER TABLE users ADD COLUMN nickname VARCHAR(50) NULL;

-- 为昵称字段创建索引
CREATE INDEX idx_users_nickname ON users (nickname); 