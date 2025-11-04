-- 为 user_salary_settings 表添加 organization_id 字段（如果不存在）
SET @column_exists_uss = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_salary_settings'
    AND COLUMN_NAME = 'organization_id'
);

SET @add_column_sql = IF(@column_exists_uss = 0,
    'ALTER TABLE user_salary_settings ADD COLUMN organization_id BIGINT NULL COMMENT ''所属机构ID'' AFTER user_id',
    'SELECT ''Column organization_id already exists in user_salary_settings'' AS message'
);

PREPARE stmt FROM @add_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 将现有工资设置关联到用户的机构
UPDATE user_salary_settings uss
JOIN users u ON uss.user_id = u.id
SET uss.organization_id = u.organization_id
WHERE u.organization_id IS NOT NULL;

-- 先删除依赖旧唯一索引的外键约束（如果存在）
SET @constraint_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_salary_settings'
    AND COLUMN_NAME = 'user_id'
    AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @drop_fk_sql = IF(@constraint_name IS NOT NULL,
    CONCAT('ALTER TABLE user_salary_settings DROP FOREIGN KEY ', @constraint_name),
    'SELECT 1'
);

PREPARE stmt FROM @drop_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 删除旧的唯一索引（如果存在）
SET @old_uk_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_salary_settings'
    AND INDEX_NAME = 'uk_user_id'
);

SET @drop_old_uk_sql = IF(@old_uk_exists > 0,
    'ALTER TABLE user_salary_settings DROP INDEX uk_user_id',
    'SELECT ''Index uk_user_id does not exist'' AS message'
);

PREPARE stmt FROM @drop_old_uk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加新的组合唯一索引（如果不存在）
SET @new_uk_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_salary_settings'
    AND INDEX_NAME = 'uk_user_organization'
);

SET @add_new_uk_sql = IF(@new_uk_exists = 0,
    'ALTER TABLE user_salary_settings ADD UNIQUE KEY uk_user_organization (user_id, organization_id)',
    'SELECT ''Index uk_user_organization already exists'' AS message'
);

PREPARE stmt FROM @add_new_uk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加机构ID索引提高查询性能（如果不存在）
SET @org_idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_salary_settings'
    AND INDEX_NAME = 'idx_user_salary_settings_organization_id'
);

SET @add_org_idx_sql = IF(@org_idx_exists = 0,
    'CREATE INDEX idx_user_salary_settings_organization_id ON user_salary_settings (organization_id)',
    'SELECT ''Index idx_user_salary_settings_organization_id already exists'' AS message'
);

PREPARE stmt FROM @add_org_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加外键约束（如果不存在）
SET @fk_exists_uss = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_salary_settings'
    AND CONSTRAINT_NAME = 'fk_user_salary_settings_organization'
);

SET @add_fk_sql = IF(@fk_exists_uss = 0,
    'ALTER TABLE user_salary_settings ADD CONSTRAINT fk_user_salary_settings_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE',
    'SELECT ''Foreign key fk_user_salary_settings_organization already exists'' AS message'
);

PREPARE stmt FROM @add_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- 为 salary_system_settings 表添加 organization_id 字段（如果不存在）
SET @column_exists_sss = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'salary_system_settings'
    AND COLUMN_NAME = 'organization_id'
);

SET @add_column_sql2 = IF(@column_exists_sss = 0,
    'ALTER TABLE salary_system_settings ADD COLUMN organization_id BIGINT NULL COMMENT ''所属机构ID'' AFTER id',
    'SELECT ''Column organization_id already exists in salary_system_settings'' AS message'
);

PREPARE stmt FROM @add_column_sql2;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 将现有的系统设置复制给每个机构（每个机构独立的记薪周期）
INSERT INTO salary_system_settings (organization_id, salary_start_day, salary_end_day, salary_pay_day, description)
SELECT 
    o.id,
    s.salary_start_day,
    s.salary_end_day,
    s.salary_pay_day,
    CONCAT('机构【', o.name, '】的记薪周期')
FROM organizations o
CROSS JOIN (
    SELECT salary_start_day, salary_end_day, salary_pay_day
    FROM salary_system_settings
    WHERE organization_id IS NULL
    LIMIT 1
) s
WHERE NOT EXISTS (
    SELECT 1 FROM salary_system_settings ss WHERE ss.organization_id = o.id
);

-- 删除旧的没有机构ID的记录
DELETE FROM salary_system_settings WHERE organization_id IS NULL;

-- 添加索引提高查询性能（如果不存在）
SET @sss_idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'salary_system_settings'
    AND INDEX_NAME = 'idx_salary_system_settings_organization_id'
);

SET @add_sss_idx_sql = IF(@sss_idx_exists = 0,
    'CREATE INDEX idx_salary_system_settings_organization_id ON salary_system_settings (organization_id)',
    'SELECT ''Index idx_salary_system_settings_organization_id already exists'' AS message'
);

PREPARE stmt FROM @add_sss_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加外键约束（如果不存在）
SET @fk_exists_sss = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'salary_system_settings'
    AND CONSTRAINT_NAME = 'fk_salary_system_settings_organization'
);

SET @add_fk_sss_sql = IF(@fk_exists_sss = 0,
    'ALTER TABLE salary_system_settings ADD CONSTRAINT fk_salary_system_settings_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE',
    'SELECT ''Foreign key fk_salary_system_settings_organization already exists'' AS message'
);

PREPARE stmt FROM @add_fk_sss_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加唯一约束，每个机构只能有一条记薪周期设置（如果不存在）
SET @sss_uk_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'salary_system_settings'
    AND INDEX_NAME = 'uk_organization_id'
);

SET @add_sss_uk_sql = IF(@sss_uk_exists = 0,
    'ALTER TABLE salary_system_settings ADD UNIQUE KEY uk_organization_id (organization_id)',
    'SELECT ''Unique key uk_organization_id already exists'' AS message'
);

PREPARE stmt FROM @add_sss_uk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

