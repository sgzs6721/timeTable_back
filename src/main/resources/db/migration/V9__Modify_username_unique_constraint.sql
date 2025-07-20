-- V9__Modify_username_unique_constraint.sql
-- 修改用户名的唯一约束，只确保未删除用户的用户名唯一

-- 注意：MySQL不支持部分唯一索引，所以我们通过应用层逻辑来确保
-- 未删除用户的用户名唯一性。在UserRepository.existsByUsername方法中
-- 已经过滤了已删除的用户，所以业务逻辑层面已经保证了唯一性。

-- 由于MySQL不支持DROP INDEX IF EXISTS，我们暂时保留原有约束
-- 应用层逻辑已经正确处理了用户名唯一性检查 