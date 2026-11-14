#!/bin/bash

# 测试报告生成脚本
REPORT_DIR="test-reports"
mkdir -p $REPORT_DIR

BASE_URL="http://localhost:8082"
API_KEY="Bearer gw-default-key-12345"

echo "=== 开始测试 ==="
echo "Base URL: $BASE_URL"
echo "API Key: $API_KEY"
echo ""

# 测试 1: 认证测试
echo "测试 1: 无认证访问"
curl -s -w "\nHTTP_CODE:%{http_code}\n" $BASE_URL/v1/models > $REPORT_DIR/test1-no-auth.txt 2>&1
echo "完成"

echo "测试 2: 错误认证"
curl -s -w "\nHTTP_CODE:%{http_code}\n" -H "Authorization: Bearer wrong-key" $BASE_URL/v1/models > $REPORT_DIR/test2-wrong-auth.txt 2>&1
echo "完成"

echo "测试 3: 正确认证 - 获取模型列表"
curl -s -w "\nHTTP_CODE:%{http_code}\n" -H "Authorization: $API_KEY" $BASE_URL/v1/models > $REPORT_DIR/test3-models.txt 2>&1
echo "完成"

echo "测试 4: DeepSeek 聊天"
curl -s -w "\nHTTP_CODE:%{http_code}\n" -X POST $BASE_URL/v1/chat/completions \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"deepseek-chat","messages":[{"role":"user","content":"Hello, say hi in one sentence"}],"stream":false}' \
  --max-time 30 > $REPORT_DIR/test4-deepseek-chat.txt 2>&1
echo "完成"

echo "测试 5: 自动路由聊天（不指定模型）"
curl -s -w "\nHTTP_CODE:%{http_code}\n" -X POST $BASE_URL/v1/chat/completions \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"messages":[{"role":"user","content":"Hi"}],"stream":false}' \
  --max-time 30 > $REPORT_DIR/test5-auto-route.txt 2>&1
echo "完成"

echo "测试 6: 不存在的模型"
curl -s -w "\nHTTP_CODE:%{http_code}\n" -X POST $BASE_URL/v1/chat/completions \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"non-existing-model","messages":[{"role":"user","content":"test"}],"stream":false}' \
  --max-time 10 > $REPORT_DIR/test6-invalid-model.txt 2>&1
echo "完成"

echo "测试 7: 流式响应"
curl -s -X POST $BASE_URL/v1/chat/completions \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"deepseek-chat","messages":[{"role":"user","content":"Count 1 to 3"}],"stream":true}' \
  --max-time 30 > $REPORT_DIR/test7-streaming.txt 2>&1
echo "完成"

echo "测试 8: LK888 图像生成"
curl -s -w "\nHTTP_CODE:%{http_code}\n" -X POST $BASE_URL/v1/images/generations \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"tt-image-2.5","prompt":"a cute cat","n":1,"size":"1024x1024"}' \
  --max-time 90 > $REPORT_DIR/test8-lk888-image.txt 2>&1
echo "完成"

echo "测试 9: 按能力过滤模型"
curl -s -w "\nHTTP_CODE:%{http_code}\n" -H "Authorization: $API_KEY" "$BASE_URL/v1/models?capability=CHAT" > $REPORT_DIR/test9-filter-chat.txt 2>&1
curl -s -w "\nHTTP_CODE:%{http_code}\n" -H "Authorization: $API_KEY" "$BASE_URL/v1/models?capability=IMAGE_GEN" > $REPORT_DIR/test9-filter-image.txt 2>&1
echo "完成"

echo "测试 10: 查询单个模型"
curl -s -w "\nHTTP_CODE:%{http_code}\n" -H "Authorization: $API_KEY" $BASE_URL/v1/models/deepseek-chat > $REPORT_DIR/test10-single-model.txt 2>&1
echo "完成"

echo ""
echo "=== 测试完成 ==="
echo "测试结果保存在: $REPORT_DIR/"
