-- V48__Create_role_permissions_table.sql
-- 创建角色权限表

CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL COMMENT '机构ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：USER/ADMIN',
    menu_permissions JSON NULL COMMENT '顶部菜单权限配置',
    action_permissions JSON NULL COMMENT '右上角功能菜单权限配置',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_role (organization_id, role),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_role_permissions_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限表';

-- 为默认机构插入默认权限配置（所有菜单都可见）
INSERT INTO role_permissions (organization_id, role, menu_permissions, action_permissions) VALUES
(1, 'ADMIN', 
 '{"dashboard":true,"todo":true,"customer":true,"mySchedule":true,"myStudents":true,"myHours":true,"mySalary":true}',
 '{"refresh":true,"admin":true,"archived":true,"profile":true,"guide":true,"logout":true}'),
(1, 'USER', 
 '{"dashboard":true,"todo":true,"customer":true,"mySchedule":true,"myStudents":true,"myHours":true,"mySalary":true}',
 '{"refresh":true,"archived":true,"profile":true,"guide":true,"logout":true}');

