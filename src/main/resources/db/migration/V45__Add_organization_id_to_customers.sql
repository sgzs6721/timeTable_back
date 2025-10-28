-- V45__Add_organization_id_to_customers.sql
-- 为客户表添加机构ID字段

ALTER TABLE customers 
ADD COLUMN organization_id BIGINT NULL COMMENT '所属机构ID';

-- 将现有客户关联到创建者的机构
UPDATE customers c
INNER JOIN users u ON c.created_by = u.id
SET c.organization_id = u.organization_id
WHERE c.organization_id IS NULL;

-- 添加索引
CREATE INDEX idx_customers_organization_id ON customers (organization_id);

