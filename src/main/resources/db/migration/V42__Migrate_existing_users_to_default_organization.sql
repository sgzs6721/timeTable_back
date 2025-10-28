-- V42__Migrate_existing_users_to_default_organization.sql
-- 将现有用户迁移到默认机构

-- 获取默认机构ID并更新现有用户
UPDATE users u
SET u.organization_id = (SELECT id FROM organizations WHERE code = 'DEFAULT_ORG' LIMIT 1)
WHERE u.organization_id IS NULL;

-- 验证迁移结果（记录日志）
-- 如果这个查询返回0，说明迁移成功
SELECT CONCAT('未分配机构的用户数量: ', COUNT(*)) as migration_check 
FROM users 
WHERE organization_id IS NULL;

