#!/bin/bash
# Gateway API 完整自测脚本

BASE_URL="http://localhost:8082"
API_KEY="Bearer gw-default-key-12345"
PASS=0
FAIL=0

green() { echo -e "\033[32m$1\033[0m"; }
red() { echo -e "\033[31m$1\033[0m"; }
yellow() { echo -e "\033[33m$1\033[0m"; }

assert_status() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    if [ "$actual" = "$expected" ]; then
        green "[PASS] $test_name (HTTP $actual)"
        PASS=$((PASS + 1))
    else
        red "[FAIL] $test_name (expected $expected, got $actual)"
        FAIL=$((FAIL + 1))
    fi
}

assert_contains() {
    local test_name="$1"
    local expected="$2"
    local body="$3"
    if echo "$body" | grep -q "$expected"; then
        green "[PASS] $test_name (contains: $expected)"
        PASS=$((PASS + 1))
    else
        red "[FAIL] $test_name (missing: $expected)"
        red "  body: $body"
        FAIL=$((FAIL + 1))
    fi
}

echo "============================================"
echo "  AI Gateway 完整自测"
echo "  $(date)"
echo "============================================"
echo ""

# ---- Test 1: 认证测试 - 无 token ----
yellow "[Test 1] GET /v1/models - 无认证"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/v1/models")
STATUS=$(echo "$RESP" | tail -1)
assert_status "no auth returns 401" "401" "$STATUS"
echo ""

# ---- Test 2: 认证测试 - 错误 token ----
yellow "[Test 2] GET /v1/models - 错误 token"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/v1/models" -H "Authorization: Bearer wrong-token")
STATUS=$(echo "$RESP" | tail -1)
assert_status "wrong token returns 401" "401" "$STATUS"
echo ""

# ---- Test 3: 模型列表查询 ----
yellow "[Test 3] GET /v1/models - 列出所有模型"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/v1/models" -H "Authorization: $API_KEY")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "models list returns 200" "200" "$STATUS"
assert_contains "models list has deepseek-chat" "deepseek-chat" "$BODY"
assert_contains "models list has gpt-4o" "gpt-4o" "$BODY"
assert_contains "models list has dall-e-3" "dall-e-3" "$BODY"
echo ""

# ---- Test 4: 按能力过滤模型 ----
yellow "[Test 4] GET /v1/models?capability=CHAT"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/v1/models?capability=CHAT" -H "Authorization: $API_KEY")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "filter by CHAT returns 200" "200" "$STATUS"
assert_contains "CHAT filter includes deepseek-chat" "deepseek-chat" "$BODY"
echo ""

# ---- Test 5: 聊天 - 指定存在的模型 ----
yellow "[Test 5] POST /v1/chat/completions - 指定 deepseek-chat"
RESP=$(curl -s -w "\n%{http_code}" --max-time 30 \
    -X POST "$BASE_URL/v1/chat/completions" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json; charset=utf-8" \
    -d '{"model":"deepseek-chat","messages":[{"role":"user","content":"hello"}],"stream":false}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "deepseek-chat returns 200" "200" "$STATUS"
assert_contains "response has choices" "choices" "$BODY"
assert_contains "response has content" "content" "$BODY"
echo ""

# ---- Test 6: 聊天 - 不指定模型（自动路由） ----
yellow "[Test 6] POST /v1/chat/completions - 不指定模型"
RESP=$(curl -s -w "\n%{http_code}" --max-time 30 \
    -X POST "$BASE_URL/v1/chat/completions" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json; charset=utf-8" \
    -d '{"messages":[{"role":"user","content":"hi"}],"stream":false}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "auto route returns 200" "200" "$STATUS"
assert_contains "auto route has choices" "choices" "$BODY"
echo ""

# ---- Test 7: 聊天 - 指定不存在的模型（应该报错，不兜底） ----
yellow "[Test 7] POST /v1/chat/completions - 不存在的模型"
RESP=$(curl -s -w "\n%{http_code}" --max-time 10 \
    -X POST "$BASE_URL/v1/chat/completions" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json; charset=utf-8" \
    -d '{"model":"non-existing-model","messages":[{"role":"user","content":"test"}],"stream":false}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "non-existing model returns 400" "400" "$STATUS"
assert_contains "error message contains Model not found" "Model not found" "$BODY"
echo ""

# ---- Test 8: 流式响应 ----
yellow "[Test 8] POST /v1/chat/completions - 流式响应"
RESP=$(curl -s --max-time 30 \
    -X POST "$BASE_URL/v1/chat/completions" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json; charset=utf-8" \
    -d '{"model":"deepseek-chat","messages":[{"role":"user","content":"count 1 to 3"}],"stream":true}')
if echo "$RESP" | grep -q "data:"; then
    green "[PASS] streaming returns SSE data"
    PASS=$((PASS + 1))
    if echo "$RESP" | grep -q "DONE"; then
        green "[PASS] streaming ends with [DONE]"
        PASS=$((PASS + 1))
    else
        red "[FAIL] streaming missing [DONE] signal"
        FAIL=$((FAIL + 1))
    fi
else
    red "[FAIL] streaming response has no SSE data"
    red "  body: $(echo "$RESP" | head -5)"
    FAIL=$((FAIL + 1))
fi
echo ""

# ---- Test 9: 图像生成 ----
yellow "[Test 9] POST /v1/images/generations - DALL-E-3"
RESP=$(curl -s -w "\n%{http_code}" --max-time 60 \
    -X POST "$BASE_URL/v1/images/generations" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json; charset=utf-8" \
    -d '{"model":"dall-e-3","prompt":"a cute cat","n":1,"size":"1024x1024"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$STATUS" = "200" ] || [ "$STATUS" = "400" ]; then
    green "[PASS] image gen endpoint responded (HTTP $STATUS)"
    PASS=$((PASS + 1))
else
    red "[FAIL] image gen endpoint unexpected status: $STATUS"
    red "  body: $BODY"
    FAIL=$((FAIL + 1))
fi
echo ""

# ---- Test 10: 单个模型查询 ----
yellow "[Test 10] GET /v1/models/deepseek-chat"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/v1/models/deepseek-chat" -H "Authorization: $API_KEY")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "single model returns 200" "200" "$STATUS"
assert_contains "model id is deepseek-chat" "deepseek-chat" "$BODY"
echo ""

# ---- Test 11: 查询不存在的模型 ----
yellow "[Test 11] GET /v1/models/non-existing"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/v1/models/non-existing" -H "Authorization: $API_KEY")
STATUS=$(echo "$RESP" | tail -1)
assert_status "non-existing model returns 404" "404" "$STATUS"
echo ""

# ---- Test 12: 检查 LangSmith 链路日志 ----
yellow "[Test 12] 检查 LangSmith 链路追踪"
if grep -q "Sent run to LangSmith" "D:/soft/漫画/noval_fj/java/gateway/gateway.log"; then
    green "[PASS] LangSmith trace sent successfully"
    PASS=$((PASS + 1))
else
    red "[FAIL] LangSmith trace not found in logs"
    FAIL=$((FAIL + 1))
fi
echo ""

# ---- Test 13: 检查请求日志 ----
yellow "[Test 13] 检查 Provider 请求日志"
if grep -q "Provider Request Start" "D:/soft/漫画/noval_fj/java/gateway/gateway.log"; then
    green "[PASS] Provider request log found"
    PASS=$((PASS + 1))
else
    red "[FAIL] Provider request log not found"
    FAIL=$((FAIL + 1))
fi
echo ""

# ---- 汇总 ----
echo "============================================"
echo "  测试结果汇总"
echo "============================================"
TOTAL=$((PASS + FAIL))
green "  通过: $PASS / $TOTAL"
if [ "$FAIL" -gt 0 ]; then
    red "  失败: $FAIL / $TOTAL"
else
    green "  失败: 0 / $TOTAL"
fi
echo "============================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
exit 0
