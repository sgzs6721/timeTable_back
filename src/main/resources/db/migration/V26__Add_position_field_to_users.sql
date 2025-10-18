-- V26__Add_position_field_to_users.sql
-- 为用户表添加职位字段

ALTER TABLE users
ADD COLUMN position VARCHAR(50) NULL COMMENT '职位：教练(COACH)、销售(SALES)、前台(RECEPTIONIST)';

-- 为现有的普通用户设置默认职位为教练
UPDATE users 
SET position = 'COACH' 
WHERE role = 'USER' AND position IS NULL;

-- 为管理员设置默认职位为空
UPDATE users 
SET position = NULL 
WHERE role = 'ADMIN';

-- 创建索引
CREATE INDEX idx_users_position ON users (position);

