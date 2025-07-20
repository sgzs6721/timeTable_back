-- 添加用户状态字段
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));

-- 为状态字段创建索引
CREATE INDEX idx_users_status ON users (status);

-- 更新现有用户状态为已批准
UPDATE users SET status = 'APPROVED' WHERE status = 'PENDING'; 