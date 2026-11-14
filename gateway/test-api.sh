#!/bin/bash
# Gateway Integration Test Script

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
echo "  AI Gateway Integration Tests"
echo "  $(date)"
echo "============================================"
echo ""

# ---- Test 1: Health check - Models endpoint ----
yellow "[Test 1] GET /v1/models - list all models"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/v1/models" -H "Authorization: $API_KEY")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "models list returns 200" "200" "$STATUS"
assert_contains "models list has deepseek-chat" "deepseek-chat" "$BODY"
assert_contains "models list has gpt-4o" "gpt-4o" "$BODY"
assert_contains "models list has claude" "claude" "$BODY"
echo ""

# ---- Test 2: Auth required ----
yellow "[Test 2] GET /v1/models without auth - expect 401"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/v1/models")
STATUS=$(echo "$RESP" | tail -1)
assert_status "no auth returns 401" "401" "$STATUS"
echo ""

# ---- Test 3: Filter by capability ----
yellow "[Test 3] GET /v1/models?capability=CHAT"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/v1/models?capability=CHAT" -H "Authorization: $API_KEY")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "filter by CHAT returns 200" "200" "$STATUS"
assert_contains "CHAT filter includes deepseek-chat" "deepseek-chat" "$BODY"
echo ""

# ---- Test 4: Filter IMAGE_GEN ----
yellow "[Test 4] GET /v1/models?capability=IMAGE_GEN"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/v1/models?capability=IMAGE_GEN" -H "Authorization: $API_KEY")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "filter by IMAGE_GEN returns 200" "200" "$STATUS"
assert_contains "IMAGE_GEN includes dall-e-3" "dall-e-3" "$BODY"
echo ""

# ---- Test 5: Get single model ----
yellow "[Test 5] GET /v1/models/deepseek-chat"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/v1/models/deepseek-chat" -H "Authorization: $API_KEY")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "single model returns 200" "200" "$STATUS"
assert_contains "model id is deepseek-chat" "deepseek-chat" "$BODY"
echo ""

# ---- Test 6: Get non-existing model ----
yellow "[Test 6] GET /v1/models/non-existing"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/v1/models/non-existing" -H "Authorization: $API_KEY")
STATUS=$(echo "$RESP" | tail -1)
assert_status "non-existing model returns 404" "404" "$STATUS"
echo ""

# ---- Test 7: DeepSeek chat (Chinese content, UTF-8 test) ----
yellow "[Test 7] POST /v1/chat/completions - DeepSeek chat (Chinese UTF-8)"
RESP=$(curl -s -w "\n%{http_code}" --max-time 30 \
    -X POST "$BASE_URL/v1/chat/completions" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json; charset=utf-8" \
    -d "{\"model\":\"deepseek-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"nihao\"}],\"stream\":false}")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "deepseek chat returns 200" "200" "$STATUS"
assert_contains "response has choices" "choices" "$BODY"
assert_contains "response has content" "content" "$BODY"
echo ""

# ---- Test 8: Auto route chat (no model specified) ----
yellow "[Test 8] POST /v1/chat/completions - auto route (no model)"
RESP=$(curl -s -w "\n%{http_code}" --max-time 30 \
    -X POST "$BASE_URL/v1/chat/completions" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json; charset=utf-8" \
    -d "{\"messages\":[{\"role\":\"user\",\"content\":\"say hello in one word\"}]}" 2>&1)
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "auto route chat returns 200" "200" "$STATUS"
assert_contains "auto route has choices" "choices" "$BODY"
echo ""

# ---- Test 9: Image gen request ----
yellow "[Test 9] POST /v1/images/generations"
RESP=$(curl -s -w "\n%{http_code}" --max-time 60 \
    -X POST "$BASE_URL/v1/images/generations" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json; charset=utf-8" \
    -d "{\"model\":\"dall-e-3\",\"prompt\":\"a cute cat\",\"n\":1,\"size\":\"1024x1024\"}" 2>&1)
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
# Image gen may fail if no OpenAI key, just check it doesn't 500
if [ "$STATUS" = "200" ] || [ "$STATUS" = "400" ]; then
    green "[PASS] image gen endpoint responded (HTTP $STATUS)"
    PASS=$((PASS + 1))
else
    red "[FAIL] image gen endpoint unexpected status: $STATUS"
    red "  body: $BODY"
    FAIL=$((FAIL + 1))
fi
echo ""

# ---- Test 10: DeepSeek streaming ----
yellow "[Test 10] POST /v1/chat/completions - DeepSeek streaming"
RESP=$(curl -s --max-time 30 \
    -X POST "$BASE_URL/v1/chat/completions" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json; charset=utf-8" \
    -d "{\"model\":\"deepseek-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"count 1 to 3\"}],\"stream\":true}" 2>&1)
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

# ---- Summary ----
echo "============================================"
echo "  Test Results"
echo "============================================"
TOTAL=$((PASS + FAIL))
green "  PASS: $PASS / $TOTAL"
if [ "$FAIL" -gt 0 ]; then
    red "  FAIL: $FAIL / $TOTAL"
else
    green "  FAIL: 0 / $TOTAL"
fi
echo "============================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
exit 0
