-- V43__Add_organization_id_to_timetables.sql
-- 为课表表添加机构ID字段

ALTER TABLE timetables 
ADD COLUMN organization_id BIGINT NULL COMMENT '所属机构ID';

-- 将现有课表关联到对应教练的机构
UPDATE timetables t
INNER JOIN users u ON t.user_id = u.id
SET t.organization_id = u.organization_id
WHERE t.organization_id IS NULL;

-- 添加索引
CREATE INDEX idx_timetables_organization_id ON timetables (organization_id);

