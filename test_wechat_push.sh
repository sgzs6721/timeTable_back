#!/bin/bash

echo "=========================================="
echo "  微信待办推送功能测试"
echo "=========================================="
echo ""

# 数据库配置
DB_HOST="121.36.91.199"
DB_USER="timetable"
DB_PASS="Leilei*0217"
DB_NAME="timetable_db"

# 步骤1：查询可用用户
echo "步骤 1/5: 查询已绑定微信的用户..."
echo "---"

RESULT=$(mysql -h$DB_HOST -u$DB_USER -p$DB_PASS $DB_NAME -s -N -e "
SELECT 
    id,
    username,
    wechat_openid
FROM users 
WHERE wechat_openid IS NOT NULL 
LIMIT 1;
")

if [ -z "$RESULT" ]; then
    echo "❌ 没有找到已绑定微信的用户"
    echo "请先使用微信登录系统"
    exit 1
fi

USER_ID=$(echo "$RESULT" | awk '{print $1}')
USERNAME=$(echo "$RESULT" | awk '{print $2}')
OPENID=$(echo "$RESULT" | awk '{print $3}')

echo "✅ 找到测试用户："
echo "   用户ID: $USER_ID"
echo "   用户名: $USERNAME"
echo "   OpenID: ${OPENID:0:20}..."
echo ""

# 获取机构ID
ORG_ID=$(mysql -h$DB_HOST -u$DB_USER -p$DB_PASS $DB_NAME -s -N -e "
SELECT organization_id FROM users WHERE id = $USER_ID;
")

# 步骤2：创建测试待办
echo "步骤 2/5: 创建测试待办..."
echo "---"

mysql -h$DB_HOST -u$DB_USER -p$DB_PASS $DB_NAME -e "
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
    '【测试】跟进意向客户',
    CURDATE(),
    CURTIME(),
    'CUSTOMER_FOLLOW_UP',
    'PENDING',
    0,
    $USER_ID,
    $ORG_ID,
    'PENDING',
    0,
    NOW(),
    NOW(),
    0
);
" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ 测试待办创建成功"
    
    TODO_ID=$(mysql -h$DB_HOST -u$DB_USER -p$DB_PASS $DB_NAME -s -N -e "
    SELECT id FROM todos 
    WHERE customer_name = '测试客户-张三'
      AND deleted = 0
    ORDER BY created_at DESC 
    LIMIT 1;
    ")
    
    echo "   待办ID: $TODO_ID"
else
    echo "❌ 创建待办失败"
    exit 1
fi
echo ""

# 步骤3：触发推送
echo "步骤 3/5: 触发微信推送..."
echo "---"

# 尝试调用测试接口
API_URL="https://timetable.devtesting.top/timetable/api/test/wechat-push/trigger"

echo "正在调用推送接口..."
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_URL" 2>/dev/null)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" == "200" ]; then
    echo "✅ 推送任务已触发"
else
    echo "⚠️  接口调用失败（HTTP $HTTP_CODE），等待定时任务执行..."
    echo "   定时任务将在5分钟内自动执行"
fi
echo ""

# 步骤4：等待并检查结果
echo "步骤 4/5: 等待推送完成..."
echo "---"
echo "等待 10 秒..."

for i in {10..1}; do
    echo -ne "  $i 秒...\r"
    sleep 1
done
echo "          "
echo ""

# 步骤5：查看推送结果
echo "步骤 5/5: 查看推送结果..."
echo "=========================================="

PUSH_RESULT=$(mysql -h$DB_HOST -u$DB_USER -p$DB_PASS $DB_NAME -s -N -e "
SELECT 
    push_status,
    pushed_at,
    push_error_message
FROM todos 
WHERE id = $TODO_ID;
")

PUSH_STATUS=$(echo "$PUSH_RESULT" | awk '{print $1}')
PUSHED_AT=$(echo "$PUSH_RESULT" | awk '{print $2,$3}')
ERROR_MSG=$(echo "$PUSH_RESULT" | cut -d$'\t' -f3)

echo ""
if [ "$PUSH_STATUS" == "PUSHED" ]; then
    echo "🎉🎉🎉 推送成功！"
    echo ""
    echo "推送详情："
    echo "  - 状态: ✅ 已推送"
    echo "  - 时间: $PUSHED_AT"
    echo "  - 待办ID: $TODO_ID"
    echo ""
    echo "📱 请检查用户微信（$USERNAME）是否收到消息"
    echo ""
elif [ "$PUSH_STATUS" == "FAILED" ]; then
    echo "❌ 推送失败"
    echo ""
    echo "失败详情："
    echo "  - 状态: ❌ 推送失败"
    echo "  - 错误信息: $ERROR_MSG"
    echo ""
    echo "常见问题："
    echo "  1. 用户未关注服务号"
    echo "  2. OpenID 错误"
    echo "  3. 模板ID配置错误"
    echo "  4. Access Token 获取失败"
    echo ""
elif [ "$PUSH_STATUS" == "PENDING" ]; then
    echo "⏳ 待办仍在等待推送"
    echo ""
    echo "可能原因："
    echo "  1. 定时任务还未执行（每5分钟执行一次）"
    echo "  2. 推送功能未启用"
    echo "  3. 应用未启动"
    echo ""
    echo "建议："
    echo "  - 查看应用日志: tail -f /root/logs/supervisor/timetable_back.out.log"
    echo "  - 手动触发: curl $API_URL"
fi

echo "=========================================="
echo ""
echo "详细信息查询："
echo "  数据库记录:"
mysql -h$DB_HOST -u$DB_USER -p$DB_PASS $DB_NAME -t -e "
SELECT 
    id,
    customer_name,
    content,
    push_status AS '推送状态',
    pushed_at AS '推送时间',
    push_retry_count AS '重试次数',
    push_error_message AS '错误信息'
FROM todos 
WHERE id = $TODO_ID;
"

echo ""
echo "清理测试数据命令："
echo "  mysql -h$DB_HOST -u$DB_USER -p'$DB_PASS' $DB_NAME -e \"UPDATE todos SET deleted=1 WHERE id=$TODO_ID;\""
echo ""

