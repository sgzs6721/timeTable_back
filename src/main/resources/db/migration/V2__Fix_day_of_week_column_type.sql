-- 修复 schedules 表的 day_of_week 列类型
-- 将 ENUM 类型改为 VARCHAR 类型以支持 jOOQ

ALTER TABLE schedules 
MODIFY COLUMN day_of_week VARCHAR(20) NOT NULL;

-- 确保现有数据的完整性
-- 如果有无效数据，可以运行以下语句进行清理
-- UPDATE schedules SET day_of_week = 'MONDAY' WHERE day_of_week NOT IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'); 