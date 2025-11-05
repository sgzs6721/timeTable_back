-- 为客户状态历史表添加体验完成字段
ALTER TABLE customer_status_history 
ADD COLUMN trial_completed BOOLEAN NULL DEFAULT FALSE COMMENT '体验是否已完成';

-- 为体验完成字段创建索引
CREATE INDEX idx_customer_status_history_trial_completed ON customer_status_history (trial_completed);

