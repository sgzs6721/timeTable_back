-- 创建周实例表 - 用于存储从固定课表生成的当前周实例
CREATE TABLE weekly_instances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_timetable_id BIGINT NOT NULL,  -- 关联的固定课表ID
    week_start_date DATE NOT NULL,          -- 周开始日期（周一）
    week_end_date DATE NOT NULL,            -- 周结束日期（周日）
    year_week VARCHAR(7) NOT NULL,          -- 年-周格式：2025-03
    is_current BOOLEAN NOT NULL DEFAULT FALSE,  -- 是否为当前周实例
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 生成时间
    last_synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 最后同步时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (template_timetable_id) REFERENCES timetables(id) ON DELETE CASCADE,
    -- 确保每个课表在每周只有一个实例
    UNIQUE KEY uk_template_week (template_timetable_id, year_week)
);

-- 创建索引
CREATE INDEX idx_weekly_instances_template_id ON weekly_instances (template_timetable_id);
CREATE INDEX idx_weekly_instances_week_dates ON weekly_instances (week_start_date, week_end_date);
CREATE INDEX idx_weekly_instances_year_week ON weekly_instances (year_week);
CREATE INDEX idx_weekly_instances_is_current ON weekly_instances (is_current);

-- 创建周实例课程表 - 存储具体的课程安排
CREATE TABLE weekly_instance_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    weekly_instance_id BIGINT NOT NULL,     -- 所属周实例ID
    template_schedule_id BIGINT NULL,       -- 源固定课表课程ID（用于同步）
    student_name VARCHAR(100) NOT NULL,
    subject VARCHAR(100),
    day_of_week VARCHAR(10) NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    schedule_date DATE NOT NULL,             -- 具体日期
    note TEXT,
    is_manual_added BOOLEAN NOT NULL DEFAULT FALSE,  -- 是否为手动添加（非模板同步）
    is_modified BOOLEAN NOT NULL DEFAULT FALSE,      -- 是否被手动修改过
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (weekly_instance_id) REFERENCES weekly_instances(id) ON DELETE CASCADE,
    FOREIGN KEY (template_schedule_id) REFERENCES schedules(id) ON DELETE SET NULL
);

-- 为周实例课程表创建索引
CREATE INDEX idx_weekly_instance_schedules_instance_id ON weekly_instance_schedules (weekly_instance_id);
CREATE INDEX idx_weekly_instance_schedules_template_id ON weekly_instance_schedules (template_schedule_id);
CREATE INDEX idx_weekly_instance_schedules_student_name ON weekly_instance_schedules (student_name);
CREATE INDEX idx_weekly_instance_schedules_day_of_week ON weekly_instance_schedules (day_of_week);
CREATE INDEX idx_weekly_instance_schedules_time ON weekly_instance_schedules (start_time, end_time);
CREATE INDEX idx_weekly_instance_schedules_date ON weekly_instance_schedules (schedule_date);

-- 添加课表类型标识字段到现有课表表，用于区分固定课表和实例课表视图
ALTER TABLE timetables ADD COLUMN timetable_type VARCHAR(20) NOT NULL DEFAULT 'TEMPLATE' 
    CHECK (timetable_type IN ('TEMPLATE', 'INSTANCE_VIEW')) 
    COMMENT '课表类型：TEMPLATE=固定课表模板，INSTANCE_VIEW=实例视图';

-- 为课表类型字段创建索引
CREATE INDEX idx_timetables_type ON timetables (timetable_type);
