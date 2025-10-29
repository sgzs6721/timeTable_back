-- V51__Update_role_permissions_for_custom_roles.sql
-- 更新 role_permissions 表以支持自定义角色

-- 先删除旧数据
DELETE FROM role_permissions;

-- 添加新字段 role_id 来关联 organization_roles
ALTER TABLE role_permissions 
ADD COLUMN role_id BIGINT NULL COMMENT '角色ID，关联 organization_roles 表' AFTER organization_id;

-- 将原来的 role 字段改为可为空（用于过渡）
ALTER TABLE role_permissions 
MODIFY COLUMN role VARCHAR(50) NULL COMMENT '角色代码（已废弃，使用 role_id）';

-- 为默认机构的各个角色插入默认权限配置
INSERT INTO role_permissions (organization_id, role_id, role, menu_permissions, action_permissions)
SELECT 
    1 as organization_id,
    id as role_id,
    role_code as role,
    CASE role_code
        WHEN 'COACH' THEN '{"dashboard":true,"todo":true,"customer":false,"mySchedule":true,"myStudents":true,"myHours":true,"mySalary":true}'
        WHEN 'SALES' THEN '{"dashboard":true,"todo":true,"customer":true,"mySchedule":false,"myStudents":false,"myHours":false,"mySalary":true}'
        WHEN 'RECEPTIONIST' THEN '{"dashboard":true,"todo":true,"customer":false,"mySchedule":false,"myStudents":false,"myHours":false,"mySalary":false}'
        WHEN 'MANAGER' THEN '{"dashboard":true,"todo":true,"customer":true,"mySchedule":true,"myStudents":true,"myHours":true,"mySalary":true}'
    END as menu_permissions,
    CASE role_code
        WHEN 'COACH' THEN '{"refresh":true,"admin":false,"archived":true,"profile":true,"guide":true,"logout":true}'
        WHEN 'SALES' THEN '{"refresh":true,"admin":false,"archived":false,"profile":true,"guide":true,"logout":true}'
        WHEN 'RECEPTIONIST' THEN '{"refresh":true,"admin":false,"archived":false,"profile":true,"guide":true,"logout":true}'
        WHEN 'MANAGER' THEN '{"refresh":true,"admin":true,"archived":true,"profile":true,"guide":true,"logout":true}'
    END as action_permissions
FROM organization_roles
WHERE organization_id = 1;

-- 添加外键约束
ALTER TABLE role_permissions
ADD CONSTRAINT fk_role_permissions_role_id 
FOREIGN KEY (role_id) REFERENCES organization_roles(id) ON DELETE CASCADE;

-- 添加索引
ALTER TABLE role_permissions
ADD INDEX idx_role_permissions_role_id (role_id);

