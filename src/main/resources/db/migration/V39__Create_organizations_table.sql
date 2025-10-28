-- V39__Create_organizations_table.sql
-- 创建机构表

CREATE TABLE IF NOT EXISTS organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '机构名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '机构代码',
    address VARCHAR(255) NULL COMMENT '详细地址',
    contact_phone VARCHAR(20) NULL COMMENT '联系电话',
    contact_person VARCHAR(50) NULL COMMENT '负责人',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
    settings JSON NULL COMMENT '机构配置',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_organizations_code (code),
    INDEX idx_organizations_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构表';

-- 插入默认机构（兜底机构，确保现有数据有归属）
INSERT INTO organizations (name, code, address, status) VALUES
('总部', 'DEFAULT_ORG', '默认机构', 'ACTIVE');

-- 可以预置一些示例机构
INSERT INTO organizations (name, code, address, contact_phone, status) VALUES
('A 旋风乒乓球培训_中关村店', 'ORG_ZGC', '北京市海淀区中关村大街1号', '010-12345678', 'ACTIVE'),
('A 旋风乒乓球培训_亚运村店', 'ORG_YYC', '北京市朝阳区亚运村路1号', '010-87654321', 'ACTIVE');

