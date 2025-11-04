-- 为 user_salary_settings 表添加 organization_id 字段
ALTER TABLE user_salary_settings
ADD COLUMN organization_id BIGINT NULL COMMENT '所属机构ID' AFTER user_id;

-- 将现有工资设置关联到用户的机构
UPDATE user_salary_settings uss
JOIN users u ON uss.user_id = u.id
SET uss.organization_id = u.organization_id
WHERE u.organization_id IS NOT NULL;

-- 添加索引提高查询性能
CREATE INDEX idx_user_salary_settings_organization_id ON user_salary_settings (organization_id);

-- 添加外键约束
ALTER TABLE user_salary_settings
ADD CONSTRAINT fk_user_salary_settings_organization
FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;

-- 修改唯一约束，改为用户ID+机构ID组合唯一
ALTER TABLE user_salary_settings
DROP INDEX uk_user_id;

ALTER TABLE user_salary_settings
ADD UNIQUE KEY uk_user_organization (user_id, organization_id);


-- 为 salary_system_settings 表添加 organization_id 字段
ALTER TABLE salary_system_settings
ADD COLUMN organization_id BIGINT NULL COMMENT '所属机构ID' AFTER id;

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

-- 添加索引提高查询性能
CREATE INDEX idx_salary_system_settings_organization_id ON salary_system_settings (organization_id);

-- 添加外键约束
ALTER TABLE salary_system_settings
ADD CONSTRAINT fk_salary_system_settings_organization
FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;

-- 添加唯一约束，每个机构只能有一条记薪周期设置
ALTER TABLE salary_system_settings
ADD UNIQUE KEY uk_organization_id (organization_id);

