-- 创建客户状态流转记录表
CREATE TABLE customer_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    from_status VARCHAR(50) COMMENT '原状态',
    to_status VARCHAR(50) NOT NULL COMMENT '新状态',
    notes TEXT COMMENT '备注信息',
    created_by BIGINT NOT NULL COMMENT '操作人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户状态流转记录表';

