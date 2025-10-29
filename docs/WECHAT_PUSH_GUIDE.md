# 微信服务号待办推送功能使用指南

## 功能概述

该功能实现了通过微信服务号向用户推送待办提醒的功能。系统会定时扫描到达提醒时间的待办事项，并通过微信模板消息推送给对应的用户。

## 核心功能

1. **定时扫描推送**：每5分钟扫描一次需要推送的待办事项
2. **智能重试机制**：推送失败时会自动重试，最多重试3次
3. **推送状态管理**：记录每条待办的推送状态、推送时间和失败原因
4. **Token缓存**：微信 Access Token 自动缓存，避免频繁请求

## 配置说明

### 1. 配置文件 (application.yml)

```yaml
wechat:
  app-id: wx966893c31aa9dddd  # 微信服务号 AppID
  app-secret: 14b4184e440acf0994b5395d441c4019  # 微信服务号 AppSecret
  
  # 微信服务号配置（用于模板消息推送）
  mp:
    enabled: true  # 是否启用模板消息推送
    template-id: YOUR_TEMPLATE_ID  # 待办提醒模板ID，需要在微信公众平台申请
    token-cache-enabled: true  # 是否缓存 Access Token
    token-cache-time: 7000  # Access Token 缓存时间（秒），微信官方有效期7200秒
    push-retry-max: 3  # 推送失败最大重试次数
```

### 2. 微信公众平台配置

#### 步骤1：申请模板消息

1. 登录 [微信公众平台](https://mp.weixin.qq.com/)
2. 进入"功能" -> "模板消息"
3. 搜索并添加 "待办提醒" 相关模板，或自定义模板

#### 建议的模板格式：

```
{{first.DATA}}
客户姓名：{{keyword1.DATA}}
待办内容：{{keyword2.DATA}}
提醒时间：{{keyword3.DATA}}
联系电话：{{keyword4.DATA}}
{{remark.DATA}}
```

#### 步骤2：获取模板ID

添加模板后会得到一个模板ID（格式如：`xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`），将此ID配置到 `application.yml` 的 `template-id` 字段。

### 3. 用户绑定微信

用户必须先绑定微信账号才能接收推送。系统已经支持微信登录，用户登录后会自动保存 `wechat_openid`。

## 数据库变更

新增了以下字段到 `todos` 表：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| push_status | VARCHAR(20) | 推送状态：PENDING-待推送，PUSHED-已推送，FAILED-推送失败 |
| pushed_at | TIMESTAMP | 推送时间 |
| push_error_message | TEXT | 推送失败原因 |
| push_retry_count | INT | 推送重试次数 |

## 核心类说明

### 1. WechatMpService
- **路径**: `com.timetable.service.WechatMpService`
- **功能**: 
  - 获取微信 Access Token（带缓存）
  - 发送模板消息
  - 构建待办提醒消息模板

### 2. TodoPushScheduledTask
- **路径**: `com.timetable.task.TodoPushScheduledTask`
- **功能**: 
  - 定时任务：每5分钟执行一次
  - 扫描需要推送的待办
  - 调用微信接口推送消息
  - 更新推送状态

### 3. TodoRepository（新增方法）
- `findTodosToPush(maxRetryCount)`: 查询需要推送的待办
- `updatePushSuccess(id)`: 更新推送成功状态
- `updatePushFailed(id, errorMessage)`: 更新推送失败状态

## 推送逻辑

### 待办推送条件

一条待办会被推送，需要同时满足：

1. **时间条件**：
   - 提醒日期 < 当前日期，或
   - 提醒日期 = 当前日期 且 提醒时间 ≤ 当前时间

2. **状态条件**：
   - 待办状态不是 "COMPLETED"（已完成）
   - 推送状态为 "PENDING"（待推送）或 "FAILED"（推送失败且重试次数 < 最大重试次数）

3. **用户条件**：
   - 用户存在
   - 用户已绑定微信（wechat_openid 不为空）

### 推送流程

```
1. 定时任务启动（每5分钟）
   ↓
2. 查询符合条件的待办
   ↓
3. 遍历每条待办
   ↓
4. 检查用户是否绑定微信
   ↓
5. 构建模板消息
   ↓
6. 调用微信API发送
   ↓
7. 更新推送状态（成功/失败）
   ↓
8. 继续下一条（间隔100ms）
```

## 测试方法

### 1. 手动触发推送

可以通过调用定时任务服务的手动方法进行测试：

```java
@Autowired
private TodoPushScheduledTask todoPushScheduledTask;

// 手动触发
todoPushScheduledTask.manualPushTodoReminders();
```

### 2. 创建测试待办

```sql
-- 创建一条立即需要推送的待办
INSERT INTO todos (
    customer_name, 
    content, 
    reminder_date, 
    reminder_time, 
    created_by, 
    push_status
) VALUES (
    '测试客户', 
    '这是一条测试待办', 
    CURDATE(), 
    CURTIME(), 
    1,  -- 你的用户ID
    'PENDING'
);
```

### 3. 查看推送日志

推送结果会记录在日志中：

```
[INFO] 开始执行定时任务：扫描待办并推送微信提醒
[INFO] 找到 X 条待办需要推送
[INFO] 待办 ID: X 推送成功，msgid: XXXXXX
[INFO] 定时任务执行完成：成功推送 X 条，失败 X 条
```

### 4. 查看推送状态

```sql
-- 查看待办的推送状态
SELECT 
    id,
    customer_name,
    content,
    reminder_date,
    reminder_time,
    push_status,
    pushed_at,
    push_retry_count,
    push_error_message
FROM todos
WHERE deleted = 0
ORDER BY created_at DESC;
```

## 常见问题

### Q1: 推送失败，错误码 40001
**原因**: Access Token 失效或错误  
**解决**: 检查 AppID 和 AppSecret 是否正确配置

### Q2: 推送失败，错误码 40003
**原因**: OpenID 错误或用户未关注公众号  
**解决**: 确保用户已关注服务号，且 OpenID 正确

### Q3: 推送失败，错误码 47003
**原因**: 模板ID错误  
**解决**: 检查 `template-id` 配置是否正确

### Q4: 用户未收到推送
**检查清单**:
1. 用户是否绑定了微信（wechat_openid 是否存在）
2. 用户是否关注了服务号
3. 待办的推送状态是什么（PENDING/PUSHED/FAILED）
4. 查看日志中是否有推送记录和错误信息

### Q5: 如何禁用推送功能
在 `application.yml` 中设置：
```yaml
wechat:
  mp:
    enabled: false
```

## 注意事项

1. **服务号要求**: 必须使用微信服务号，订阅号不支持模板消息
2. **用户关注**: 用户必须关注了你的服务号才能接收推送
3. **推送频率**: 定时任务每5分钟执行一次，可根据需要调整
4. **Token有效期**: Access Token 有效期为2小时，系统会自动缓存和刷新
5. **消息限制**: 微信对模板消息有发送频率限制，避免短时间大量推送

## 扩展建议

### 1. 自定义推送时间
可以修改定时任务的 cron 表达式：

```java
@Scheduled(cron = "0 */10 * * * *")  // 改为每10分钟执行一次
```

### 2. 添加更多模板字段
在 `WechatMpService.buildTodoReminderMessage()` 方法中可以添加更多字段：

```java
message.addData("keyword5", "额外信息", "#173177");
```

### 3. 推送状态通知
可以在前端展示待办的推送状态，让用户知道是否已推送成功。

## 相关文件

- **数据库迁移**: `V51__Add_wechat_push_fields_to_todos.sql`
- **配置类**: `WechatMpConfig.java`
- **服务类**: `WechatMpService.java`
- **定时任务**: `TodoPushScheduledTask.java`
- **DTO**: `WechatAccessTokenResponse.java`, `WechatTemplateMessage.java`, `WechatTemplateMessageResponse.java`
- **Repository**: `TodoRepository.java`（新增推送相关方法）

