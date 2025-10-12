-- 创建学员操作记录表
CREATE TABLE IF NOT EXISTS student_operation_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型：RENAME, DELETE, ASSIGN_ALIAS, MERGE',
    old_name VARCHAR(255) NOT NULL COMMENT '原学员姓名',
    new_name VARCHAR(255) COMMENT '新学员姓名或操作描述',
    details TEXT COMMENT '操作详情，JSON格式',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_coach_id (coach_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at)
) COMMENT='学员操作记录表';