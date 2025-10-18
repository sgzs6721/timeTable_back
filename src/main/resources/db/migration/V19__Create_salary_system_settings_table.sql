-- 创建工资系统设置表
CREATE TABLE IF NOT EXISTS salary_system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_start_day INT NOT NULL DEFAULT 1 COMMENT '记薪开始日(1-31)',
    salary_end_day INT NOT NULL DEFAULT 31 COMMENT '记薪结束日(1-31)',
    salary_pay_day INT NOT NULL DEFAULT 5 COMMENT '工资发放日(1-31)',
    description TEXT COMMENT '工资计算说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工资系统设置表';

-- 插入默认设置
INSERT INTO salary_system_settings (salary_start_day, salary_end_day, salary_pay_day, description) 
VALUES (1, 31, 5, '默认记薪周期：每月1号到31号，工资在次月5号发放');
