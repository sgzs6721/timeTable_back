-- V44__Add_organization_id_to_schedules.sql
-- 为课程安排表添加机构ID字段

ALTER TABLE schedules 
ADD COLUMN organization_id BIGINT NULL COMMENT '所属机构ID';

-- 将现有课程关联到对应课表的机构
UPDATE schedules s
INNER JOIN timetables t ON s.timetable_id = t.id
SET s.organization_id = t.organization_id
WHERE s.organization_id IS NULL;

-- 添加索引
CREATE INDEX idx_schedules_organization_id ON schedules (organization_id);

