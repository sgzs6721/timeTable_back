-- 为周实例课程表添加请假相关字段
ALTER TABLE weekly_instance_schedules 
ADD COLUMN is_on_leave BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否请假',
ADD COLUMN leave_reason TEXT NULL COMMENT '请假原因',
ADD COLUMN leave_requested_at TIMESTAMP NULL COMMENT '请假申请时间';

-- 为请假相关字段创建索引
CREATE INDEX idx_weekly_instance_schedules_leave ON weekly_instance_schedules (is_on_leave);
CREATE INDEX idx_weekly_instance_schedules_leave_date ON weekly_instance_schedules (leave_requested_at);
