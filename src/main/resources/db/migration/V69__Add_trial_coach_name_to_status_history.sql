-- 添加 trial_coach_name 字段到 customer_status_history 表
ALTER TABLE customer_status_history 
ADD COLUMN trial_coach_name VARCHAR(100) COMMENT '体验教练名称' AFTER trial_coach_id;
