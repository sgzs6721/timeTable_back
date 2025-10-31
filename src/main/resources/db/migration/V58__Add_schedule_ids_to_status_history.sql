-- Add schedule IDs to customer_status_history table
-- 在状态历史表中添加课表ID字段，用于直接删除体验课程

ALTER TABLE customer_status_history 
ADD COLUMN trial_schedule_id BIGINT NULL COMMENT '体验课程ID（schedules表或weekly_instance_schedules表的ID）',
ADD COLUMN trial_timetable_id BIGINT NULL COMMENT '体验课程所属课表ID（仅普通课程需要）',
ADD COLUMN trial_source_type VARCHAR(50) NULL COMMENT '课程来源类型：schedule或weekly_instance';

-- 添加索引
CREATE INDEX idx_status_history_trial_schedule ON customer_status_history(trial_schedule_id);
CREATE INDEX idx_status_history_trial_timetable ON customer_status_history(trial_timetable_id);

