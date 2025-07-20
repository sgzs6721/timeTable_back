-- V10__Create_student_names_table.sql
-- 创建学生姓名记录表，用于智能联想功能

CREATE TABLE student_names (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    usage_count INT NOT NULL DEFAULT 1,
    first_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_student_names_user_id ON student_names (user_id);
CREATE INDEX idx_student_names_name ON student_names (name);
CREATE INDEX idx_student_names_usage_count ON student_names (usage_count);
CREATE INDEX idx_student_names_last_used ON student_names (last_used_at);

-- 创建唯一约束：同一用户下的学生姓名唯一
CREATE UNIQUE INDEX idx_student_names_user_name ON student_names (user_id, name); 