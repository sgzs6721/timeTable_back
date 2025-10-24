-- V36__Add_wechat_fields_to_users.sql
-- 为用户表添加微信相关字段和手机号字段

-- 添加微信相关字段
ALTER TABLE users ADD COLUMN wechat_openid VARCHAR(64) NULL COMMENT '微信OpenID';
ALTER TABLE users ADD COLUMN wechat_unionid VARCHAR(64) NULL COMMENT '微信UnionID';
ALTER TABLE users ADD COLUMN wechat_avatar VARCHAR(512) NULL COMMENT '微信头像URL';
ALTER TABLE users ADD COLUMN wechat_sex TINYINT NULL COMMENT '性别：0未知，1男性，2女性';
ALTER TABLE users ADD COLUMN wechat_province VARCHAR(50) NULL COMMENT '省份';
ALTER TABLE users ADD COLUMN wechat_city VARCHAR(50) NULL COMMENT '城市';
ALTER TABLE users ADD COLUMN wechat_country VARCHAR(50) NULL COMMENT '国家';

-- 添加手机号字段
ALTER TABLE users ADD COLUMN phone VARCHAR(20) NULL COMMENT '手机号';

-- 创建索引
CREATE UNIQUE INDEX idx_users_wechat_openid ON users (wechat_openid);
CREATE INDEX idx_users_wechat_unionid ON users (wechat_unionid);
CREATE INDEX idx_users_phone ON users (phone);

-- 创建复合索引用于查询已绑定手机号的微信用户
CREATE INDEX idx_users_wechat_phone ON users (wechat_openid, phone);





