-- 插入默认管理员用户 (密码: admin123)
MERGE INTO users (username, password_hash, role) KEY (username) VALUES 
('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN');

-- 插入测试用户 (密码: user123)
MERGE INTO users (username, password_hash, role) KEY (username) VALUES 
('testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER'); 