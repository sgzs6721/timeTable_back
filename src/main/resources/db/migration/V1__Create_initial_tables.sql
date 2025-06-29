-- 创建用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_name (name),
    INDEX idx_created_at (created_at)
);

-- 创建课程安排表
CREATE TABLE schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timetable_id BIGINT NOT NULL,
    student_name VARCHAR(100) NOT NULL,
    subject VARCHAR(100),
    day_of_week ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    week_number INT NULL COMMENT '周次，对于日期范围课表使用',
    schedule_date DATE NULL COMMENT '具体日期，对于日期范围课表使用',
    note TEXT COMMENT '备注信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (timetable_id) REFERENCES timetables(id) ON DELETE CASCADE,
    INDEX idx_timetable_id (timetable_id),
    INDEX idx_student_name (student_name),
    INDEX idx_day_of_week (day_of_week),
    INDEX idx_time (start_time, end_time),
    INDEX idx_week_number (week_number),
    INDEX idx_schedule_date (schedule_date)
);

-- 创建语音处理记录表
CREATE TABLE voice_processing_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    timetable_id BIGINT NOT NULL,
    original_audio_path VARCHAR(500),
    transcribed_text TEXT,
    processed_result JSON,
    status ENUM('PROCESSING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PROCESSING',
    error_message TEXT,
    processing_time_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (timetable_id) REFERENCES timetables(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_timetable_id (timetable_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- 创建文本处理记录表
CREATE TABLE text_processing_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    timetable_id BIGINT NOT NULL,
    input_text TEXT NOT NULL,
    processed_result JSON,
    status ENUM('PROCESSING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PROCESSING',
    error_message TEXT,
    processing_time_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (timetable_id) REFERENCES timetables(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_timetable_id (timetable_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- 插入默认管理员用户 (密码: admin123)
INSERT INTO users (username, email, password_hash, role) VALUES 
('admin', 'admin@timetable.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN');

-- 插入测试用户 (密码: user123)
INSERT INTO users (username, email, password_hash, role) VALUES 
('testuser', 'test@timetable.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER'); 