-- 创建用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 为用户表创建索引
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);

-- 创建课表表
CREATE TABLE timetables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_weekly BOOLEAN NOT NULL DEFAULT FALSE,
    start_date DATE NULL,
    end_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 为课表表创建索引
CREATE INDEX idx_timetables_user_id ON timetables (user_id);
CREATE INDEX idx_timetables_name ON timetables (name);
CREATE INDEX idx_timetables_created_at ON timetables (created_at);

-- 创建课程安排表
CREATE TABLE schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timetable_id BIGINT NOT NULL,
    student_name VARCHAR(100) NOT NULL,
    subject VARCHAR(100),
    day_of_week VARCHAR(10) NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    week_number INT NULL,
    schedule_date DATE NULL,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (timetable_id) REFERENCES timetables(id) ON DELETE CASCADE
);

-- 为课程安排表创建索引
CREATE INDEX idx_schedules_timetable_id ON schedules (timetable_id);
CREATE INDEX idx_schedules_student_name ON schedules (student_name);
CREATE INDEX idx_schedules_day_of_week ON schedules (day_of_week);
CREATE INDEX idx_schedules_time ON schedules (start_time, end_time);
CREATE INDEX idx_schedules_week_number ON schedules (week_number);
CREATE INDEX idx_schedules_schedule_date ON schedules (schedule_date);

-- 创建语音处理记录表
CREATE TABLE voice_processing_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    timetable_id BIGINT NOT NULL,
    original_audio_path VARCHAR(500),
    transcribed_text TEXT,
    processed_result TEXT,
    status VARCHAR(15) NOT NULL DEFAULT 'PROCESSING' CHECK (status IN ('PROCESSING', 'SUCCESS', 'FAILED')),
    error_message TEXT,
    processing_time_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (timetable_id) REFERENCES timetables(id) ON DELETE CASCADE
);

-- 为语音处理记录表创建索引
CREATE INDEX idx_voice_logs_user_id ON voice_processing_logs (user_id);
CREATE INDEX idx_voice_logs_timetable_id ON voice_processing_logs (timetable_id);
CREATE INDEX idx_voice_logs_status ON voice_processing_logs (status);
CREATE INDEX idx_voice_logs_created_at ON voice_processing_logs (created_at);

-- 创建文本处理记录表
CREATE TABLE text_processing_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    timetable_id BIGINT NOT NULL,
    input_text TEXT NOT NULL,
    processed_result TEXT,
    status VARCHAR(15) NOT NULL DEFAULT 'PROCESSING' CHECK (status IN ('PROCESSING', 'SUCCESS', 'FAILED')),
    error_message TEXT,
    processing_time_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (timetable_id) REFERENCES timetables(id) ON DELETE CASCADE
);

-- 为文本处理记录表创建索引
CREATE INDEX idx_text_logs_user_id ON text_processing_logs (user_id);
CREATE INDEX idx_text_logs_timetable_id ON text_processing_logs (timetable_id);
CREATE INDEX idx_text_logs_status ON text_processing_logs (status);
CREATE INDEX idx_text_logs_created_at ON text_processing_logs (created_at);

-- 插入默认管理员用户 (密码: admin123)
INSERT INTO users (username, email, password_hash, role) VALUES 
('admin', 'admin@timetable.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN');

-- 插入测试用户 (密码: user123)
INSERT INTO users (username, email, password_hash, role) VALUES 
('testuser', 'test@timetable.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER'); 