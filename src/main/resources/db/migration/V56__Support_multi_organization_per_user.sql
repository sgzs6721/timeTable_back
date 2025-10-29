-- V56__Support_multi_organization_per_user.sql
-- 支持一个微信用户申请多个机构

-- 1. 删除旧的 wechat_openid 唯一索引
DROP INDEX idx_users_wechat_openid ON users;

-- 2. 创建新的组合唯一索引，允许同一个 wechat_openid 在不同机构有多条记录
-- 但同一个 wechat_openid 在同一个机构只能有一条记录
CREATE UNIQUE INDEX idx_users_wechat_openid_org ON users (wechat_openid, organization_id);

-- 3. 为了快速查询某个微信用户的所有机构，保留普通索引
CREATE INDEX idx_users_wechat_openid_only ON users (wechat_openid);

