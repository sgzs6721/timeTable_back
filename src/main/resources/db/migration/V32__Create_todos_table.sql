CREATE TABLE IF NOT EXISTS todos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '待办ID',
    customer_id BIGINT COMMENT '关联的客户ID',
    customer_name VARCHAR(100) COMMENT '客户姓名',
    content TEXT NOT NULL COMMENT '待办内容',
    reminder_date DATE COMMENT '提醒日期',
    type VARCHAR(50) DEFAULT 'CUSTOMER_FOLLOW_UP' COMMENT '待办类型',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '待办状态：PENDING-待办，COMPLETED-已完成',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BOOLEAN DEFAULT FALSE COMMENT '是否已删除',
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_created_by (created_by),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_reminder_date (reminder_date),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办表';

