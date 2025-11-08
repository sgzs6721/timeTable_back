# 机构申请审批接口文档

## 接口信息

**接口路径**: `/api/organization-requests/approve`  
**请求方法**: `POST`  
**接口说明**: 审批用户加入机构的申请（同意或拒绝）

---

## 权限要求

- **需要认证**: ✅ 是（需要Bearer Token）
- **权限要求**: ADMIN（管理员权限）

---

## 请求参数

### Headers
```
Authorization: Bearer {token}
Content-Type: application/json
```

### Request Body (JSON)

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| requestId | Long | ✅ | 申请ID |
| approved | Boolean | ✅ | 审批结果：true-同意，false-拒绝 |
| rejectReason | String | 条件必填 | 拒绝理由（当approved=false时必填） |
| defaultRole | String | ❌ | 默认角色（同意时可选，默认为USER） |
| defaultPosition | String | ❌ | 默认职位（同意时可选，默认为COACH） |

### 参数说明

1. **requestId**: 要审批的申请记录ID
2. **approved**: 
   - `true`: 同意申请，用户将加入机构
   - `false`: 拒绝申请，需要提供拒绝理由
3. **rejectReason**: 拒绝申请时，必须提供拒绝理由说明
4. **defaultRole**: 同意申请时，可设置用户在该机构的默认角色
   - 可选值: `USER`, `ADMIN` 等
   - 默认值: `USER`
5. **defaultPosition**: 同意申请时，可设置用户的默认职位
   - 可选值: `COACH`, `ASSISTANT` 等
   - 默认值: `COACH`

---

## 请求示例

### 示例1: 同意申请

```json
{
  "requestId": 123,
  "approved": true,
  "defaultRole": "USER",
  "defaultPosition": "COACH"
}
```

### 示例2: 拒绝申请

```json
{
  "requestId": 123,
  "approved": false,
  "rejectReason": "该用户不符合机构要求"
}
```

---

## 响应格式

### 成功响应

**状态码**: `200 OK`

```json
{
  "success": true,
  "message": "申请已批准",
  "data": {
    "id": 123,
    "userId": 456,
    "organizationId": 789,
    "status": "APPROVED",
    "applyReason": "申请加入机构",
    "approveTime": "2024-01-01T10:00:00",
    "approverId": 100,
    "rejectReason": null,
    "defaultRole": "USER",
    "defaultPosition": "COACH"
  }
}
```

### 错误响应

#### 1. 未授权
**状态码**: `400 Bad Request`
```json
{
  "success": false,
  "message": "未提供认证信息"
}
```

#### 2. 无权限
**状态码**: `400 Bad Request`
```json
{
  "success": false,
  "message": "无权限审批申请"
}
```

#### 3. 拒绝时未提供理由
**状态码**: `400 Bad Request`
```json
{
  "success": false,
  "message": "拒绝申请时必须提供拒绝理由"
}
```

#### 4. 申请不存在或状态异常
**状态码**: `400 Bad Request`
```json
{
  "success": false,
  "message": "申请不存在或已处理"
}
```

#### 5. 服务器错误
**状态码**: `500 Internal Server Error`
```json
{
  "success": false,
  "message": "审批申请失败"
}
```

---

## 业务逻辑

### 同意申请流程
1. 验证管理员权限
2. 检查申请状态（必须是PENDING状态）
3. 更新用户信息：
   - 设置用户的 `organizationId`
   - 设置默认角色和职位（如果提供）
4. 更新申请记录：
   - 状态改为 `APPROVED`
   - 记录审批时间和审批人
5. 返回更新后的申请信息

### 拒绝申请流程
1. 验证管理员权限
2. 检查申请状态（必须是PENDING状态）
3. 验证拒绝理由（不能为空）
4. 更新申请记录：
   - 状态改为 `REJECTED`
   - 记录拒绝理由、审批时间和审批人
5. 返回更新后的申请信息

---

## 接口调用示例

### cURL
```bash
# 同意申请
curl -X POST "http://localhost:8089/api/organization-requests/approve" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": 123,
    "approved": true,
    "defaultRole": "USER",
    "defaultPosition": "COACH"
  }'

# 拒绝申请
curl -X POST "http://localhost:8089/api/organization-requests/approve" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": 123,
    "approved": false,
    "rejectReason": "不符合机构要求"
  }'
```

### JavaScript (fetch)
```javascript
// 同意申请
const approveRequest = async (requestId, defaultRole, defaultPosition) => {
  const response = await fetch('/api/organization-requests/approve', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      requestId: requestId,
      approved: true,
      defaultRole: defaultRole || 'USER',
      defaultPosition: defaultPosition || 'COACH'
    })
  });
  
  return await response.json();
};

// 拒绝申请
const rejectRequest = async (requestId, rejectReason) => {
  const response = await fetch('/api/organization-requests/approve', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      requestId: requestId,
      approved: false,
      rejectReason: rejectReason
    })
  });
  
  return await response.json();
};
```

---

## 注意事项

1. **权限验证**: 只有管理员(ADMIN)可以审批申请
2. **申请状态**: 只能审批状态为PENDING的申请
3. **拒绝理由**: 拒绝申请时必须提供拒绝理由，不能为空
4. **默认值**: 如果不提供defaultRole和defaultPosition，系统会使用默认值
5. **日志记录**: 所有审批操作都会记录日志，包括操作人和操作时间

---

## 相关接口

- `GET /api/organization-requests/pending` - 获取待审批申请列表
- `GET /api/organization-requests/my-request` - 获取我的申请状态
- `GET /api/organization-requests/organization/{organizationId}` - 获取机构的申请列表
- `POST /api/auth/apply-organization` - 提交机构申请
- `POST /api/auth/wechat/apply-by-code` - 通过机构代码申请

---

## 版本信息

- **接口版本**: v1.0
- **最后更新**: 2024年
- **维护者**: 开发团队


