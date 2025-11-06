-- 给 schedules 表添加 is_time_block 字段，用于标记占用时间段
ALTER TABLE schedules ADD COLUMN is_time_block BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为占用时间段（不排课）';

-- 给 weekly_instance_schedules 表添加 is_time_block 字段
ALTER TABLE weekly_instance_schedules ADD COLUMN is_time_block BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为占用时间段（不排课）';

-- 为已存在的占用时间段数据设置标记
UPDATE schedules SET is_time_block = TRUE WHERE student_name = '【占用】';
UPDATE weekly_instance_schedules SET is_time_block = TRUE WHERE student_name = '【占用】';

-- 创建索引以提高查询性能
CREATE INDEX idx_schedules_is_time_block ON schedules (is_time_block);
CREATE INDEX idx_weekly_instance_schedules_is_time_block ON weekly_instance_schedules (is_time_block);

