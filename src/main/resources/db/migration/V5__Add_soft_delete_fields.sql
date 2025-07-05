-- V5__Add_soft_delete_fields.sql
-- 为用户表和课表表添加软删除字段

-- 为用户表添加软删除字段
ALTER TABLE users ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP NULL;

-- 为课表表添加软删除字段
ALTER TABLE timetables ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE timetables ADD COLUMN deleted_at TIMESTAMP NULL;

-- 为软删除字段创建索引
CREATE INDEX idx_users_is_deleted ON users (is_deleted);
CREATE INDEX idx_timetables_is_deleted ON timetables (is_deleted);