-- 更新工资发放日约束，允许0值（表示月末）
ALTER TABLE salary_system_settings 
MODIFY COLUMN salary_pay_day INT NOT NULL DEFAULT 5 COMMENT '工资发放日 (0=月末, 1-31)';

-- 更新默认数据（如果存在的话）
UPDATE salary_system_settings 
SET salary_pay_day = 31 
WHERE salary_pay_day = 31 AND salary_pay_day IS NOT NULL;
