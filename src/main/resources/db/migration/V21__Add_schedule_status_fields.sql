-- 为周实例课程表添加课程状态字段
ALTER TABLE weekly_instance_schedules 
ADD COLUMN is_cancelled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否取消',
ADD COLUMN cancelled_reason TEXT NULL COMMENT '取消原因',
ADD COLUMN cancelled_at TIMESTAMP NULL COMMENT '取消时间';

-- 为取消相关字段创建索引
CREATE INDEX idx_weekly_instance_schedules_cancelled ON weekly_instance_schedules (is_cancelled);
CREATE INDEX idx_weekly_instance_schedules_cancelled_date ON weekly_instance_schedules (cancelled_at);
