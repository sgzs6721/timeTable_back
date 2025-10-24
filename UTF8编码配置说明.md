# UTF-8编码配置说明

## 问题描述
日志中的中文字符显示为问号，这是由于系统没有正确配置UTF-8编码导致的。

## 已完成的修改

### 1. pom.xml 配置修改

#### 添加项目编码属性
在 `<properties>` 中添加了以下配置：
```xml
<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
<maven.compiler.encoding>UTF-8</maven.compiler.encoding>
```

#### Spring Boot Maven插件配置
添加了JVM启动参数：
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <jvmArguments>-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8</jvmArguments>
    </configuration>
</plugin>
```

#### Maven编译器插件配置
添加了编译时的UTF-8编码：
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>8</source>
        <target>8</target>
        <encoding>UTF-8</encoding>
    </configuration>
</plugin>
```

### 2. application.yml 配置修改

在所有环境配置（dev、test、prod）的日志配置中添加了控制台编码：
```yaml
logging:
  charset:
    console: UTF-8  # 新增
    file: UTF-8
```

### 3. logback-spring.xml 配置文件

创建了专门的Logback配置文件，明确指定所有输出的编码为UTF-8：
- 控制台输出使用UTF-8编码
- 文件输出使用UTF-8编码
- 配置了日志滚动策略（按日期和大小）
- 针对不同环境（dev、test、prod）设置了不同的日志级别

### 4. compile_java.sh 修改

在javac编译命令中添加了 `-encoding UTF-8` 参数：
```bash
javac -encoding UTF-8 -cp "$CLASSPATH" -d target/classes ...
```

### 5. start.sh 启动脚本

创建了新的启动脚本，包含以下功能：
- 设置系统环境变量 `LANG=zh_CN.UTF-8` 和 `LC_ALL=zh_CN.UTF-8`
- 设置JVM参数 `-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8`
- 自动查找并启动jar文件

## 使用方法

### 方法1：使用Maven启动（推荐开发环境）
```bash
mvn spring-boot:run
```

### 方法2：使用启动脚本（推荐生产环境）
```bash
# 1. 先打包
mvn clean package

# 2. 使用启动脚本
./start.sh
```

### 方法3：手动启动jar包
```bash
# 设置环境变量
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8

# 启动应用
java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -jar target/timetable-backend-*.jar
```

## 日志文件位置

日志文件保存在：
- 主日志：`/root/logs/supervisor/timetable_back.out.log`
- 历史日志：`/root/logs/supervisor/timetable_back.out.2025-10-24.0.log` (按日期滚动)

查看日志命令：
```bash
# 实时查看日志
tail -f /root/logs/supervisor/timetable_back.out.log

# 查看最近100行
tail -n 100 /root/logs/supervisor/timetable_back.out.log
```

## 验证方法

1. 启动应用后，在代码中添加日志输出中文：
```java
log.info("测试中文日志：你好世界");
```

2. 查看控制台和日志文件，确认中文正常显示

3. 如果还有问题，检查：
   - 终端/控制台本身是否支持UTF-8
   - 操作系统的locale设置
   - IDE的文件编码设置

## 注意事项

1. **重新编译**：修改pom.xml后需要重新编译项目：
   ```bash
   mvn clean compile
   ```

2. **清理缓存**：如果问题仍然存在，清理Maven缓存：
   ```bash
   mvn clean
   rm -rf target/
   ```

3. **数据库编码**：确保MySQL数据库也使用UTF-8编码：
   - 数据库创建时使用：`CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`
   - 连接URL已包含：`characterEncoding=utf8&useUnicode=true`

4. **IDE设置**：确保IDE（如IntelliJ IDEA、Eclipse）的文件编码也设置为UTF-8

## 配置文件位置

- Maven配置：`pom.xml`
- Spring配置：`src/main/resources/application.yml`
- Logback配置：`src/main/resources/logback-spring.xml`
- 启动脚本：`start.sh`
- 编译脚本：`compile_java.sh`

## 相关文件

所有修改的文件都已提交，包括：
- ✅ pom.xml
- ✅ application.yml
- ✅ logback-spring.xml（新增）
- ✅ compile_java.sh
- ✅ start.sh（新增）

