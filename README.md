# 飓风乒乓培训 - 课程管理系统 (后端)

## 📖 项目简介

飓风乒乓培训课程管理系统的后端服务，基于 Spring Boot + MySQL + Flyway 构建的RESTful API服务，提供用户认证、课表管理、语音AI处理等核心功能。

## ✨ 主要功能

- 🔐 **JWT认证系统**：用户登录、注册、权限管理
- 📅 **课表管理API**：CRUD操作、数据持久化
- 🤖 **AI语音处理**：集成语音识别API
- 🔒 **Spring Security**：安全认证和授权
- 🗄️ **数据库管理**：Flyway版本控制
- 📊 **RESTful API**：标准化接口设计

## 🛠️ 技术栈

- **框架**：Spring Boot 2.7.18
- **安全**：Spring Security + JWT
- **数据库**：MySQL 8.0 + Spring Data JPA
- **版本控制**：Flyway 7.14.0
- **构建工具**：Maven 3.6+
- **开发语言**：Java 8
- **HTTP客户端**：OkHttp 4.9.3

## 📋 环境要求

- Java 8 或更高版本
- Maven 3.6+ 
- MySQL 8.0+
- Redis (可选，用于缓存)

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone [repository-url]
cd timeTable_back
```

### 2. 配置数据库

创建MySQL数据库：

```sql
CREATE DATABASE timetable_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 配置文件

修改 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/timetable_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: your_username
    password: your_password
  
  ai:
    api:
      key: your_ai_api_key
      base-url: https://api.your-ai-provider.com
```

### 4. 安装依赖并运行

```bash
# 安装依赖
mvn clean install

# 运行数据库迁移
mvn flyway:migrate

# 启动应用
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动

## 📁 项目结构

```
timeTable_back/
├── src/main/java/com/timetable/
│   ├── TimetableApplication.java    # 应用启动类
│   ├── config/                     # 配置类
│   │   ├── SecurityConfig.java     # 安全配置
│   │   ├── JwtConfig.java          # JWT配置
│   │   └── CorsConfig.java         # 跨域配置
│   ├── controller/                 # 控制器层
│   │   ├── AuthController.java     # 认证接口
│   │   ├── TimetableController.java # 课表接口
│   │   └── AdminController.java    # 管理员接口
│   ├── service/                    # 服务层
│   │   ├── UserService.java        # 用户服务
│   │   ├── TimetableService.java   # 课表服务
│   │   └── AIService.java          # AI语音服务
│   ├── repository/                 # 数据访问层
│   │   ├── UserRepository.java     # 用户仓库
│   │   ├── TimetableRepository.java # 课表仓库
│   │   └── ScheduleRepository.java  # 排课仓库
│   ├── entity/                     # 实体类
│   │   ├── User.java               # 用户实体
│   │   ├── Timetable.java          # 课表实体
│   │   └── Schedule.java           # 排课实体
│   ├── dto/                        # 数据传输对象
│   │   ├── LoginRequest.java       # 登录请求
│   │   ├── UserResponse.java       # 用户响应
│   │   └── TimetableDto.java       # 课表DTO
│   └── util/                       # 工具类
│       ├── JwtUtil.java            # JWT工具
│       └── ResponseUtil.java       # 响应工具
├── src/main/resources/
│   ├── application.yml             # 应用配置
│   └── db/migration/              # 数据库迁移脚本
│       └── V1__Create_initial_tables.sql
└── pom.xml                        # Maven配置
```

## 🗄️ 数据库设计

### 用户表 (users)
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role ENUM('user', 'admin') DEFAULT 'user',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 课表表 (timetables)
```sql
CREATE TABLE timetables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_weekly BOOLEAN DEFAULT FALSE,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 排课表 (schedules)
```sql
CREATE TABLE schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timetable_id BIGINT NOT NULL,
    student_name VARCHAR(100) NOT NULL,
    day_of_week INT NOT NULL,
    time_slot VARCHAR(20) NOT NULL,
    class_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (timetable_id) REFERENCES timetables(id)
);
```

## 📡 API接口

### 认证接口

- `POST /auth/login` - 用户登录
- `POST /auth/register` - 用户注册
- `POST /auth/logout` - 用户登出

### 课表接口

- `GET /timetables` - 获取课表列表
- `POST /timetables` - 创建课表
- `GET /timetables/{id}` - 获取课表详情
- `PUT /timetables/{id}` - 更新课表
- `DELETE /timetables/{id}` - 删除课表

### 排课接口

- `GET /timetables/{id}/schedules` - 获取排课数据
- `POST /timetables/{id}/schedules` - 添加排课
- `POST /timetables/{id}/voice-input` - 语音录入排课

### 管理员接口

- `GET /admin/users` - 获取用户列表
- `GET /admin/timetables` - 获取所有课表
- `POST /admin/merge-timetables` - 合并课表

## 🔧 配置说明

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/timetable_db
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
```

### JWT配置

```yaml
jwt:
  secret: your-secret-key
  expiration: 86400000  # 24小时
```

### AI API配置

```yaml
ai:
  api:
    key: your-api-key
    base-url: https://api.openai.com
    model: gpt-3.5-turbo
```

## 🧪 测试

### 运行单元测试

```bash
mvn test
```

### 运行集成测试

```bash
mvn integration-test
```

### API测试

使用提供的Postman集合或curl命令测试API：

```bash
# 用户登录
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# 获取课表列表
curl -X GET http://localhost:8080/timetables \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 📦 部署

### Docker部署

```dockerfile
FROM openjdk:8-jre-alpine
COPY target/timetable-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

构建和运行：

```bash
# 构建镜像
docker build -t timetable-backend .

# 运行容器
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod timetable-backend
```

### 生产环境配置

```yaml
spring:
  profiles: prod
  datasource:
    url: jdbc:mysql://your-db-host:3306/timetable_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
logging:
  level:
    com.timetable: INFO
  file:
    name: logs/timetable-backend.log
```

## 🔍 监控和日志

### 健康检查

- `GET /actuator/health` - 应用健康状态
- `GET /actuator/info` - 应用信息

### 日志配置

```yaml
logging:
  level:
    org.springframework.security: DEBUG
    com.timetable: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## 🤝 开发指南

### 代码规范

- 遵循阿里巴巴Java开发手册
- 使用Spring Boot最佳实践
- 统一异常处理和响应格式
- 完善的单元测试覆盖

### 提交规范

```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式
refactor: 重构
test: 测试相关
chore: 构建过程或辅助工具的变动
```

## 📝 更新日志

### v1.0.0 (2024-01-01)
- ✨ 初始版本发布
- 🔐 JWT认证系统
- 📅 课表管理功能
- 🤖 AI语音处理集成
- 🗄️ 数据库设计和迁移

## 📞 联系方式

- **项目地址**：[GitHub Repository]
- **问题反馈**：[Issues]
- **API文档**：[Swagger UI] (http://localhost:8080/swagger-ui.html)

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

---

**飓风乒乓培训** - 专业保障快乐提高 🏓 