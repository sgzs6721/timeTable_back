-- V51__Add_wechat_push_fields_to_todos.sql
-- 为待办表添加微信推送相关字段

ALTER TABLE todos ADD COLUMN push_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '推送状态：PENDING-待推送，PUSHED-已推送，FAILED-推送失败';
ALTER TABLE todos ADD COLUMN pushed_at TIMESTAMP NULL COMMENT '推送时间';
ALTER TABLE todos ADD COLUMN push_error_message TEXT NULL COMMENT '推送失败原因';
ALTER TABLE todos ADD COLUMN push_retry_count INT DEFAULT 0 COMMENT '推送重试次数';

-- 创建索引用于定时任务查询
CREATE INDEX idx_todos_push_status ON todos (push_status);
CREATE INDEX idx_todos_reminder_datetime ON todos (reminder_date, reminder_time);

