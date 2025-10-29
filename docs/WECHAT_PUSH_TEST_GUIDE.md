# 微信待办推送测试指南

## 🎯 测试目标

验证微信服务号模板消息推送功能是否正常工作。

## 📋 测试前准备

### 1. 确认配置已完成

检查 `application.yml` 配置：

```yaml
wechat:
  app-id: wx966893c31aa9dddd
  app-secret: 14b4184e440acf0994b5395d441c4019
  
  mp:
    enabled: true
    template-id: dBncPXrXjhyUL-dB9t8ThFJQwT01IIGn66LQYGJEkVw
```

### 2. 确认用户已绑定微信

用户必须：
- 使用微信登录过系统（有 wechat_openid）
- 关注了微信服务号

## 🧪 测试步骤

### 方式一：使用 SQL 脚本测试（推荐）

#### 步骤 1：连接数据库

使用数据库客户端（如 Navicat、DBeaver）连接到：
```
Host: 121.36.91.199
Port: 3306
Database: timetable_db
Username: timetable
Password: Leilei*0217
```

#### 步骤 2：查询可用的测试用户

运行以下 SQL：

```sql
-- 查询已绑定微信的用户
SELECT 
    id AS user_id,
    username,
    nickname,
    wechat_openid,
    organization_id
FROM users 
WHERE deleted = 0 
  AND wechat_openid IS NOT NULL 
LIMIT 5;
```

**记录查询结果**：
- `user_id`（例如：123）
- `organization_id`（例如：1）

#### 步骤 3：创建测试待办

将下面的 `@user_id` 和 `@org_id` 替换为步骤2查询到的实际值：

```sql
-- 设置变量
SET @user_id = 123;        -- 替换为实际的用户ID
SET @org_id = 1;           -- 替换为实际的机构ID

-- 创建测试待办
INSERT INTO todos (
    customer_name,
    content,
    reminder_date,
    reminder_time,
    type,
    status,
    is_read,
    created_by,
    organization_id,
    push_status,
    push_retry_count,
    created_at,
    updated_at,
    deleted
) VALUES (
    '测试客户-张三',
    '跟进意向客户',
    CURDATE(),
    CURTIME(),
    'CUSTOMER_FOLLOW_UP',
    'PENDING',
    0,
    @user_id,
    @org_id,
    'PENDING',
    0,
    NOW(),
    NOW(),
    0
);

-- 查看刚创建的待办
SELECT 
    id,
    customer_name,
    content,
    reminder_date,
    reminder_time,
    push_status
FROM todos 
WHERE customer_name = '测试客户-张三'
  AND deleted = 0
ORDER BY created_at DESC 
LIMIT 1;
```

#### 步骤 4：手动触发推送

有两种方式触发推送：

**方式 A：使用测试接口（推荐）**

在浏览器或 Postman 中访问：

```
GET https://timetable.devtesting.top/timetable/api/test/wechat-push/trigger
```

或本地测试：
```
GET http://localhost:8080/timetable/api/test/wechat-push/trigger
```

**方式 B：等待定时任务**

等待最多5分钟，定时任务会自动执行。

#### 步骤 5：查看推送结果

**方式 A：查看数据库**

```sql
-- 查看推送状态
SELECT 
    id,
    customer_name,
    content,
    push_status,
    CASE 
        WHEN push_status = 'PENDING' THEN '⏳ 待推送'
        WHEN push_status = 'PUSHED' THEN '✅ 已推送'
        WHEN push_status = 'FAILED' THEN '❌ 推送失败'
    END AS status_text,
    pushed_at,
    push_retry_count,
    push_error_message
FROM todos 
WHERE customer_name = '测试客户-张三'
  AND deleted = 0;
```

**方式 B：查看日志**

```bash
tail -f /root/logs/supervisor/timetable_back.out.log
```

成功日志：
```
[INFO] 开始执行定时任务：扫描待办并推送微信提醒
[INFO] 找到 1 条待办需要推送
[INFO] 成功获取 Access Token
[INFO] 模板消息发送成功，msgid: 123456789
[INFO] 待办 ID: X 推送成功
[INFO] 定时任务执行完成：成功推送 1 条，失败 0 条
```

**方式 C：检查微信**

用户的微信中会收到服务号消息。

### 方式二：使用 SQL 脚本文件

我已经创建了完整的测试脚本，位置：
```
/Users/sgzs/program/time_table/timeTable_back/src/test/resources/create_test_todo.sql
```

在数据库客户端中打开此文件，按照注释逐步执行即可。

## ✅ 验证推送成功

### 数据库层面

```sql
SELECT * FROM todos 
WHERE customer_name = '测试客户-张三'
  AND push_status = 'PUSHED'  -- 状态为已推送
  AND pushed_at IS NOT NULL;   -- 有推送时间
```

### 日志层面

日志中出现：
```
[INFO] 模板消息发送成功，msgid: XXXXX
[INFO] 待办 ID: X 推送成功
```

### 微信层面

用户收到服务号消息，内容格式：

```
⏰ 您有新的待办提醒

学员姓名：测试客户-张三
课程名称：跟进意向客户 (2025-10-29 15:30)

💡 点击查看详情，及时跟进处理
```

## ❌ 常见问题排查

### 问题 1：推送失败，错误码 40001

**原因**：Access Token 无效

**排查**：
1. 检查 AppID 和 AppSecret 是否正确
2. 查看日志中获取 Token 的部分

**解决**：
```sql
-- 查看配置
SELECT * FROM application.yml;
```

### 问题 2：推送失败，错误码 40003

**原因**：用户 OpenID 错误或用户未关注公众号

**排查**：
```sql
-- 检查用户的 openid
SELECT id, username, wechat_openid 
FROM users 
WHERE id = 你的用户ID;
```

**解决**：
- 确保用户已关注服务号
- 确保 wechat_openid 不为空

### 问题 3：推送失败，错误码 47003

**原因**：模板ID错误

**排查**：
- 检查配置的模板ID是否正确
- 确认模板在微信公众平台的"我的模板"中存在

**解决**：
```yaml
template-id: dBncPXrXjhyUL-dB9t8ThFJQwT01IIGn66LQYGJEkVw  # 确认此ID正确
```

### 问题 4：没有找到待推送的待办

**原因**：查询条件不满足

**排查**：
```sql
-- 检查待办的各个字段
SELECT 
    id,
    reminder_date,
    reminder_time,
    status,
    push_status,
    deleted,
    CASE 
        WHEN deleted = 1 THEN '❌ 已删除'
        WHEN status = 'COMPLETED' THEN '❌ 已完成'
        WHEN push_status = 'PUSHED' THEN '❌ 已推送'
        WHEN reminder_date > CURDATE() THEN '❌ 提醒日期未到'
        WHEN reminder_date = CURDATE() AND reminder_time > CURTIME() THEN '❌ 提醒时间未到'
        ELSE '✅ 符合推送条件'
    END AS check_result
FROM todos 
WHERE customer_name = '测试客户-张三';
```

**解决**：
- 确保 `deleted = 0`
- 确保 `status != 'COMPLETED'`
- 确保 `push_status = 'PENDING'`
- 确保提醒时间已到

### 问题 5：用户未收到微信消息

**检查清单**：

1. ✅ 用户是否关注了服务号？
2. ✅ 数据库中 push_status 是否为 PUSHED？
3. ✅ 日志中是否显示发送成功？
4. ✅ 用户是否屏蔽了公众号消息？
5. ✅ 微信是否有网络问题？

## 🧹 清理测试数据

测试完成后，可以删除测试数据：

```sql
-- 软删除（推荐）
UPDATE todos 
SET deleted = 1 
WHERE customer_name LIKE '测试客户-%';

-- 或硬删除
DELETE FROM todos 
WHERE customer_name LIKE '测试客户-%';
```

## 📊 测试报告模板

测试完成后，可以填写以下报告：

```
【微信待办推送测试报告】

测试时间：2025-10-29 15:30
测试人员：XXX

1. 配置检查
   - AppID/AppSecret：✅ 正确
   - 模板ID：✅ 已配置
   - 推送开关：✅ 已启用

2. 测试数据
   - 用户ID：123
   - 待办ID：456
   - OpenID：有/无

3. 推送结果
   - 数据库状态：PUSHED/FAILED
   - 日志记录：成功/失败
   - 微信接收：是/否

4. 问题记录
   - 问题描述：
   - 错误信息：
   - 解决方案：

5. 测试结论
   ☐ 通过
   ☐ 不通过（原因：）
```

## 🎉 测试成功标志

- ✅ 数据库中待办状态变为 `PUSHED`
- ✅ 有 `pushed_at` 推送时间
- ✅ 日志显示"推送成功"
- ✅ 用户微信收到消息
- ✅ 消息内容格式正确

## 📞 需要帮助？

如果测试过程中遇到问题，请：
1. 查看完整日志
2. 检查数据库中的 push_error_message 字段
3. 参考《WECHAT_PUSH_GUIDE.md》文档

祝测试顺利！🎊

