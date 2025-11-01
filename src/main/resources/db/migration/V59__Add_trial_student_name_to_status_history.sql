-- Add trial_student_name field to customer_status_history table
-- 添加体验人员姓名字段

ALTER TABLE customer_status_history 
ADD COLUMN trial_student_name VARCHAR(100) NULL COMMENT '体验人员姓名';

