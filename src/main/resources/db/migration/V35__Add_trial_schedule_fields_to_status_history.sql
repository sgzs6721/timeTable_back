-- Add trial schedule fields to customer_status_history table
-- 添加体验课程相关字段，即使不选教练也能保存体验时间

ALTER TABLE customer_status_history 
ADD COLUMN trial_schedule_date DATE NULL COMMENT '体验课日期',
ADD COLUMN trial_start_time TIME NULL COMMENT '体验课开始时间',
ADD COLUMN trial_end_time TIME NULL COMMENT '体验课结束时间',
ADD COLUMN trial_coach_id BIGINT NULL COMMENT '体验课教练ID（可选）';

-- 添加索引以提高查询性能
CREATE INDEX idx_status_history_trial_date ON customer_status_history(trial_schedule_date);

