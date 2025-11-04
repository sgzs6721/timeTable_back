-- 为待办表添加取消时间字段
ALTER TABLE todos ADD COLUMN cancelled_at TIMESTAMP NULL COMMENT '取消时间' AFTER completed_at;

