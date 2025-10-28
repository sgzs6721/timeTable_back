-- Add customer_id field to schedules table
-- 添加客源ID字段，用于关联体验课程到具体的客源

ALTER TABLE schedules 
ADD COLUMN customer_id BIGINT NULL COMMENT '关联的客源ID（主要用于体验课程）';

-- 添加索引以提高查询性能
CREATE INDEX idx_schedules_customer_id ON schedules(customer_id);

-- 添加外键约束（可选，如果需要强制关联）
ALTER TABLE schedules
ADD CONSTRAINT fk_schedules_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;

-- Add customer_id field to weekly_instance_schedules table
ALTER TABLE weekly_instance_schedules 
ADD COLUMN customer_id BIGINT NULL COMMENT '关联的客源ID（主要用于体验课程）';

-- 添加索引以提高查询性能
CREATE INDEX idx_weekly_instance_schedules_customer_id ON weekly_instance_schedules(customer_id);

-- 添加外键约束
ALTER TABLE weekly_instance_schedules
ADD CONSTRAINT fk_weekly_instance_schedules_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;

