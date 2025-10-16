-- 创建用户工资设置表
CREATE TABLE IF NOT EXISTS user_salary_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    base_salary DECIMAL(10, 2) DEFAULT 0.00 COMMENT '底薪',
    social_security DECIMAL(10, 2) DEFAULT 0.00 COMMENT '社保',
    hourly_rate DECIMAL(10, 2) DEFAULT 0.00 COMMENT '课时费',
    commission_rate DECIMAL(5, 2) DEFAULT 0.00 COMMENT '提成比例(%)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户工资设置表';

