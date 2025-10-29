-- V50__Create_organization_roles_table.sql
-- 创建机构自定义角色表

CREATE TABLE IF NOT EXISTS organization_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL COMMENT '机构ID',
    role_code VARCHAR(50) NOT NULL COMMENT '角色代码（英文，如 COACH, SALES）',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称（中文，如 教练、销售）',
    description VARCHAR(255) NULL COMMENT '角色描述',
    icon VARCHAR(50) NULL COMMENT '图标标识',
    color VARCHAR(20) NULL COMMENT '颜色代码',
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否为系统角色（系统角色不可删除）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_role_code (organization_id, role_code),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_org_roles (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构自定义角色表';

-- 为默认机构插入默认角色
INSERT INTO organization_roles (organization_id, role_code, role_name, description, icon, color, is_system) VALUES
(1, 'COACH', '教练', '负责课程教学的教练', 'trophy', '#52c41a', TRUE),
(1, 'SALES', '销售', '负责客户开发和销售', 'shopping', '#fa8c16', TRUE),
(1, 'RECEPTIONIST', '前台', '负责前台接待和日常事务', 'customer-service', '#722ed1', TRUE),
(1, 'MANAGER', '管理', '负责日常管理工作，拥有所有权限', 'control', '#13c2c2', TRUE);

