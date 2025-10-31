-- Add trial cancelled field to customer_status_history table
-- 添加体验课程取消标记字段

ALTER TABLE customer_status_history 
ADD COLUMN trial_cancelled BOOLEAN DEFAULT FALSE COMMENT '体验课程是否已取消';

-- 将已存在的历史记录的 trial_cancelled 设置为 FALSE
UPDATE customer_status_history 
SET trial_cancelled = FALSE 
WHERE trial_cancelled IS NULL;

-- 添加索引
CREATE INDEX idx_status_history_trial_cancelled ON customer_status_history(trial_cancelled);

