-- V46__Add_organization_id_to_other_tables.sql
-- 为其他相关表添加机构ID字段

-- 周实例表
ALTER TABLE weekly_instances 
ADD COLUMN organization_id BIGINT NULL COMMENT '所属机构ID';

UPDATE weekly_instances wi
INNER JOIN timetables t ON wi.timetable_id = t.id
SET wi.organization_id = t.organization_id
WHERE wi.organization_id IS NULL;

CREATE INDEX idx_weekly_instances_organization_id ON weekly_instances (organization_id);

-- 待办事项表
ALTER TABLE todos 
ADD COLUMN organization_id BIGINT NULL COMMENT '所属机构ID';

UPDATE todos td
INNER JOIN users u ON td.user_id = u.id
SET td.organization_id = u.organization_id
WHERE td.organization_id IS NULL;

CREATE INDEX idx_todos_organization_id ON todos (organization_id);

-- 客户状态历史表
ALTER TABLE customer_status_history 
ADD COLUMN organization_id BIGINT NULL COMMENT '所属机构ID';

UPDATE customer_status_history csh
INNER JOIN customers c ON csh.customer_id = c.id
SET csh.organization_id = c.organization_id
WHERE csh.organization_id IS NULL;

CREATE INDEX idx_customer_status_history_organization_id ON customer_status_history (organization_id);

-- 学生操作记录表
ALTER TABLE student_operation_records 
ADD COLUMN organization_id BIGINT NULL COMMENT '所属机构ID';

UPDATE student_operation_records sor
INNER JOIN users u ON sor.user_id = u.id
SET sor.organization_id = u.organization_id
WHERE sor.organization_id IS NULL;

CREATE INDEX idx_student_operation_records_organization_id ON student_operation_records (organization_id);

