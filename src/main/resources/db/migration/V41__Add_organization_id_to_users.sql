-- V41__Add_organization_id_to_users.sql
-- 为用户表添加机构ID字段

ALTER TABLE users 
ADD COLUMN organization_id BIGINT NULL COMMENT '所属机构ID';

-- 添加索引（提高查询性能）
CREATE INDEX idx_users_organization_id ON users (organization_id);

-- 注意：这里先不添加外键约束，等数据迁移完成后再添加

