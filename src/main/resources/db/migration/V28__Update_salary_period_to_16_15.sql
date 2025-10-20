-- 更新工资系统设置：将记薪周期改为每月16号到次月15号
UPDATE salary_system_settings 
SET 
    salary_start_day = 16,
    salary_end_day = 15,
    description = '记薪周期：每月16号到次月15号，工资在次月5号发放'
WHERE id = 1;

