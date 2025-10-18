-- 更新 position 字段的注释，添加管理(MANAGER)职位
ALTER TABLE users MODIFY COLUMN position VARCHAR(50) NULL COMMENT '职位：教练(COACH)、销售(SALES)、前台(RECEPTIONIST)、管理(MANAGER)';

