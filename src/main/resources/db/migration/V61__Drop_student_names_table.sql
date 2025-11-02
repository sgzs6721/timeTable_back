-- 删除student_names表（该表功能未使用）
-- 先删除student_operation_records表中的外键约束和student_id字段
ALTER TABLE student_operation_records DROP FOREIGN KEY student_operation_records_ibfk_1;
ALTER TABLE student_operation_records DROP COLUMN student_id;

-- 然后删除student_names表
DROP TABLE IF EXISTS student_names;

