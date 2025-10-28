-- V46__Add_organization_id_to_other_tables.sql
-- 为其他相关表添加机构ID字段（支持幂等执行）

-- 周实例表
SET @table_name = 'weekly_instances';
SET @column_name = 'organization_id';
SET @schema_name = DATABASE();

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = @schema_name 
     AND TABLE_NAME = @table_name 
     AND COLUMN_NAME = @column_name) > 0,
    'SELECT "Column already exists"',
    'ALTER TABLE weekly_instances ADD COLUMN organization_id BIGINT NULL COMMENT ''所属机构ID'''
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE weekly_instances wi
INNER JOIN timetables t ON wi.template_timetable_id = t.id
SET wi.organization_id = t.organization_id
WHERE wi.organization_id IS NULL;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = @schema_name 
     AND TABLE_NAME = @table_name 
     AND INDEX_NAME = 'idx_weekly_instances_organization_id') > 0,
    'SELECT "Index already exists"',
    'CREATE INDEX idx_weekly_instances_organization_id ON weekly_instances (organization_id)'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 待办事项表
SET @table_name = 'todos';
SET @column_name = 'organization_id';

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = @schema_name 
     AND TABLE_NAME = @table_name 
     AND COLUMN_NAME = @column_name) > 0,
    'SELECT "Column already exists"',
    'ALTER TABLE todos ADD COLUMN organization_id BIGINT NULL COMMENT ''所属机构ID'''
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE todos td
INNER JOIN users u ON td.created_by = u.id
SET td.organization_id = u.organization_id
WHERE td.organization_id IS NULL;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = @schema_name 
     AND TABLE_NAME = @table_name 
     AND INDEX_NAME = 'idx_todos_organization_id') > 0,
    'SELECT "Index already exists"',
    'CREATE INDEX idx_todos_organization_id ON todos (organization_id)'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 客户状态历史表
SET @table_name = 'customer_status_history';
SET @column_name = 'organization_id';

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = @schema_name 
     AND TABLE_NAME = @table_name 
     AND COLUMN_NAME = @column_name) > 0,
    'SELECT "Column already exists"',
    'ALTER TABLE customer_status_history ADD COLUMN organization_id BIGINT NULL COMMENT ''所属机构ID'''
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE customer_status_history csh
INNER JOIN customers c ON csh.customer_id = c.id
SET csh.organization_id = c.organization_id
WHERE csh.organization_id IS NULL;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = @schema_name 
     AND TABLE_NAME = @table_name 
     AND INDEX_NAME = 'idx_customer_status_history_organization_id') > 0,
    'SELECT "Index already exists"',
    'CREATE INDEX idx_customer_status_history_organization_id ON customer_status_history (organization_id)'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 学生操作记录表
SET @table_name = 'student_operation_records';
SET @column_name = 'organization_id';

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = @schema_name 
     AND TABLE_NAME = @table_name 
     AND COLUMN_NAME = @column_name) > 0,
    'SELECT "Column already exists"',
    'ALTER TABLE student_operation_records ADD COLUMN organization_id BIGINT NULL COMMENT ''所属机构ID'''
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE student_operation_records sor
INNER JOIN users u ON sor.coach_id = u.id
SET sor.organization_id = u.organization_id
WHERE sor.organization_id IS NULL;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = @schema_name 
     AND TABLE_NAME = @table_name 
     AND INDEX_NAME = 'idx_student_operation_records_organization_id') > 0,
    'SELECT "Index already exists"',
    'CREATE INDEX idx_student_operation_records_organization_id ON student_operation_records (organization_id)'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

