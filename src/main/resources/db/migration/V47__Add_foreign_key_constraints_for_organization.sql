-- V47__Add_foreign_key_constraints_for_organization.sql
-- 添加外键约束（可选，在确认数据迁移无误后执行）

-- 用户表
ALTER TABLE users 
ADD CONSTRAINT fk_users_organization 
FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- 课表表
ALTER TABLE timetables 
ADD CONSTRAINT fk_timetables_organization 
FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- 课程安排表
ALTER TABLE schedules 
ADD CONSTRAINT fk_schedules_organization 
FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- 客户表
ALTER TABLE customers 
ADD CONSTRAINT fk_customers_organization 
FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- 周实例表
ALTER TABLE weekly_instances 
ADD CONSTRAINT fk_weekly_instances_organization 
FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- 待办事项表
ALTER TABLE todos 
ADD CONSTRAINT fk_todos_organization 
FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- 客户状态历史表
ALTER TABLE customer_status_history 
ADD CONSTRAINT fk_customer_status_history_organization 
FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- 学生操作记录表
ALTER TABLE student_operation_records 
ADD CONSTRAINT fk_student_operation_records_organization 
FOREIGN KEY (organization_id) REFERENCES organizations(id);

