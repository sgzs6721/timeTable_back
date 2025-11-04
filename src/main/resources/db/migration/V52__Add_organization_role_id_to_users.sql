-- 为users表添加organization_role_id字段，用于支持自定义角色
ALTER TABLE users 
ADD COLUMN organization_role_id BIGINT NULL COMMENT '机构角色ID，用于自定义角色';

-- 添加索引提高查询性能
CREATE INDEX idx_users_organization_role_id ON users (organization_role_id);

-- 添加外键约束（允许为空，因为不是所有用户都需要分配自定义角色）
ALTER TABLE users 
ADD CONSTRAINT fk_users_organization_role 
FOREIGN KEY (organization_role_id) REFERENCES organization_roles(id) 
ON DELETE SET NULL;

