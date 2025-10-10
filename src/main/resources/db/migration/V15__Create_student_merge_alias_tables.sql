-- 学员合并表：将多个学员合并为一个显示
CREATE TABLE student_merges (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    display_name VARCHAR(100) NOT NULL COMMENT '合并后的显示名称',
    student_names TEXT NOT NULL COMMENT '被合并的学员名称，JSON数组格式',
    coach_id BIGINT NOT NULL COMMENT '所属教练ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    deleted_at TIMESTAMP NULL,
    INDEX idx_coach_id (coach_id),
    INDEX idx_display_name (display_name),
    INDEX idx_is_deleted (is_deleted)
) COMMENT='学员合并表';

-- 学员别名表：一个名称代表多个学员
CREATE TABLE student_aliases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alias_name VARCHAR(100) NOT NULL COMMENT '别名',
    student_names TEXT NOT NULL COMMENT '关联的学员名称，JSON数组格式',
    coach_id BIGINT NOT NULL COMMENT '所属教练ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    deleted_at TIMESTAMP NULL,
    INDEX idx_coach_id (coach_id),
    INDEX idx_alias_name (alias_name),
    INDEX idx_is_deleted (is_deleted)
) COMMENT='学员别名表';
