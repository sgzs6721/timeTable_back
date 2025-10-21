-- 创建客户表
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    child_name VARCHAR(100) NOT NULL COMMENT '孩子姓名',
    grade VARCHAR(20) NULL COMMENT '年级',
    parent_phone VARCHAR(20) NOT NULL COMMENT '家长电话',
    parent_relation ENUM('MOTHER', 'FATHER') NOT NULL COMMENT '家长关系：妈妈/爸爸',
    available_time TEXT NULL COMMENT '什么时候有时间',
    source VARCHAR(200) NULL COMMENT '在哪里宣传的',
    status ENUM('NEW', 'CONTACTED', 'SCHEDULED', 'PENDING_CONFIRM', 'VISITED', 'SOLD', 'RE_EXPERIENCE', 'CLOSED') NOT NULL DEFAULT 'NEW' COMMENT '客户状态',
    notes TEXT NULL COMMENT '备注信息',
    next_contact_time DATETIME NULL COMMENT '下次联系时间',
    visit_time DATETIME NULL COMMENT '上门时间',
    assigned_sales_id BIGINT NULL COMMENT '分配的销售ID',
    created_by BIGINT NOT NULL COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (assigned_sales_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户信息表';

-- 创建索引
CREATE INDEX idx_customers_status ON customers (status);
CREATE INDEX idx_customers_assigned_sales ON customers (assigned_sales_id);
CREATE INDEX idx_customers_created_by ON customers (created_by);
CREATE INDEX idx_customers_parent_phone ON customers (parent_phone);
