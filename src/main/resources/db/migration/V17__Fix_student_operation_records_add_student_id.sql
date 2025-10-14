-- V17__Fix_student_operation_records_add_student_id.sql
-- 修复学员操作记录表，添加学员ID字段以支持同名学员处理

-- 添加student_id字段，引用student_names表的id
ALTER TABLE student_operation_records 
ADD COLUMN student_id BIGINT NULL COMMENT '学员ID，引用student_names表',
ADD INDEX idx_student_id (student_id),
ADD FOREIGN KEY (student_id) REFERENCES student_names(id) ON DELETE CASCADE;

-- 注意：现有的old_name和new_name字段保留，用于向后兼容和显示
-- 新的逻辑将优先使用student_id，如果student_id为NULL则回退到名称匹配
