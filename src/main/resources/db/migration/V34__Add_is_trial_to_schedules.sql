-- Add is_trial field to schedules table
ALTER TABLE schedules ADD COLUMN is_trial TINYINT(1) DEFAULT 0 COMMENT '是否为体验课程';

-- Add is_trial field to weekly_instance_schedules table
ALTER TABLE weekly_instance_schedules ADD COLUMN is_trial TINYINT(1) DEFAULT 0 COMMENT '是否为体验课程';

