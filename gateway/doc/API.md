# AI Gateway API 接口文档

**Base URL**: `http://localhost:8082`  
**协议**: OpenAI 兼容  
**认证**: Bearer Token  
**Swagger UI**: http://localhost:8082/swagger-ui.html

---

## 认证

所有 `/v1/**` 接口需要 `Authorization` 请求头：

```
Authorization: Bearer <api-key>
```

默认 API Key: `gw-default-key-12345`

---

## 1. 聊天补全

### POST `/v1/chat/completions`

支持纯文本对话和视觉理解（多模态）。

#### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `model` | string | 否 | 模型 ID，如 `gpt-4o`, `claude-sonnet-5-5`, `gemini-2.0-flash` |
| `messages` | array | 是 | 消息列表 |
| `stream` | boolean | 否 | 是否流式响应，默认 `false` |
| `temperature` | number | 否 | 温度 (0-2) |
| `max_tokens` | integer | 否 | 最大生成 token 数 |

#### Message 结构

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `role` | string | 是 | `system` / `user` / `assistant` |
| `content` | string 或 array | 是 | 文本内容或内容片段数组（视觉模式） |

#### ContentPart 结构（视觉模式）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | string | 是 | `text` 或 `image_url` |
| `text` | string | 条件 | 当 type=text 时必填 |
| `image_url` | object | 条件 | 当 type=image_url 时必填 |
| `image_url.url` | string | 条件 | 图片 URL 或 base64 |

#### 纯文本请求示例

```bash
curl -X POST http://localhost:8082/v1/chat/completions \
  -H "Authorization: Bearer gw-default-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [
      {"role": "system", "content": "你是一个有帮助的助手"},
      {"role": "user", "content": "你好"}
    ],
    "temperature": 0.7,
    "max_tokens": 1000
  }'
```

#### 视觉理解请求示例

```bash
curl -X POST http://localhost:8082/v1/chat/completions \
  -H "Authorization: Bearer gw-default-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{
      "role": "user",
      "content": [
        {"type": "text", "text": "描述这张图片的内容"},
        {"type": "image_url", "image_url": {"url": "https://example.com/image.jpg"}}
      ]
    }]
  }'
```

#### 响应示例（非流式）

```json
{
  "id": "chatcmpl-abc123",
  "object": "chat.completion",
  "created": 1700000000,
  "model": "gpt-4o",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "你好！有什么我可以帮助你的吗？"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 20,
    "completion_tokens": 15,
    "total_tokens": 35
  }
}
```

#### 流式响应示例

设置 `"stream": true`，响应为 SSE 格式：

```
data: {"id":"chatcmpl-abc123","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"你"},"finish_reason":null}]}

data: {"id":"chatcmpl-abc123","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"好"},"finish_reason":null}]}

data: [DONE]
```

---

## 2. 图像生成

### POST `/v1/images/generations`

根据文本提示生成图像。

#### 请求参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `model` | string | 否 | dall-e-3 | 模型 ID |
| `prompt` | string | 是 | - | 图像描述 |
| `n` | integer | 否 | 1 | 生成数量 (1-10) |
| `size` | string | 否 | 1024x1024 | 尺寸：`1024x1024`, `1792x1024`, `1024x1792` |
| `response_format` | string | 否 | url | 响应格式：`url` 或 `b64_json` |

#### 请求示例

```bash
curl -X POST http://localhost:8082/v1/images/generations \
  -H "Authorization: Bearer gw-default-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "dall-e-3",
    "prompt": "A cute orange cat playing with yarn ball, digital art",
    "n": 1,
    "size": "1024x1024",
    "response_format": "url"
  }'
```

#### 响应示例

```json
{
  "created": 1700000000,
  "data": [
    {
      "url": "https://example.com/generated-image.png",
      "revised_prompt": null
    }
  ]
}
```

---

## 3. 模型列表

### GET `/v1/models`

获取所有可用模型。

#### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `capability` | string | 否 | 按能力过滤：`CHAT`, `VISION`, `IMAGE_GEN` |

#### 请求示例

```bash
# 获取所有模型
curl http://localhost:8082/v1/models \
  -H "Authorization: Bearer gw-default-key-12345"

# 只获取支持视觉的模型
curl "http://localhost:8082/v1/models?capability=VISION" \
  -H "Authorization: Bearer gw-default-key-12345"
```

#### 响应示例

```json
{
  "object": "list",
  "data": [
    {
      "id": "gpt-4o",
      "object": "model",
      "created": 1700000000,
      "owned_by": "openai",
      "name": "GPT-4o",
      "capabilities": ["CHAT", "VISION"]
    },
    {
      "id": "claude-sonnet-5-5",
      "object": "model",
      "created": 1700000000,
      "owned_by": "claude",
      "name": "Claude Sonnet 5.5",
      "capabilities": ["CHAT", "VISION"]
    },
    {
      "id": "gemini-2.0-flash",
      "object": "model",
      "created": 1700000000,
      "owned_by": "google",
      "name": "Gemini 2.0 Flash",
      "capabilities": ["CHAT", "VISION"]
    },
    {
      "id": "dall-e-3",
      "object": "model",
      "created": 1700000000,
      "owned_by": "openai",
      "name": "DALL-E 3",
      "capabilities": ["IMAGE_GEN"]
    }
  ]
}
```

---

## 4. 获取单个模型

### GET `/v1/models/{modelId}`

获取指定模型的详细信息。

#### 请求示例

```bash
curl http://localhost:8082/v1/models/gpt-4o \
  -H "Authorization: Bearer gw-default-key-12345"
```

#### 响应示例

```json
{
  "id": "gpt-4o",
  "object": "model",
  "created": 1700000000,
  "owned_by": "openai",
  "name": "GPT-4o",
  "capabilities": ["CHAT", "VISION"]
}
```

#### 错误响应（404）

```json
{
  "error": {
    "message": "Model not found",
    "type": "not_found",
    "code": null
  }
}
```

---

## 错误响应

所有错误返回统一格式：

```json
{
  "error": {
    "message": "错误描述信息",
    "type": "错误类型",
    "code": "错误代码"
  }
}
```

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（缺少或无效 API Key） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 可用模型

### OpenAI

| 模型 ID | 能力 | 说明 |
|---------|------|------|
| `gpt-4o` | CHAT, VISION | GPT-4o 多模态 |
| `gpt-4o-mini` | CHAT, VISION | GPT-4o Mini 轻量版 |
| `gpt-4-turbo` | CHAT, VISION | GPT-4 Turbo |
| `dall-e-3` | IMAGE_GEN | DALL-E 3 图像生成 |
| `dall-e-2` | IMAGE_GEN | DALL-E 2 图像生成 |

### Claude

| 模型 ID | 能力 | 说明 |
|---------|------|------|
| `claude-opus-5-5` | CHAT, VISION | Claude Opus 5.5 |
| `claude-sonnet-5-5` | CHAT, VISION | Claude Sonnet 5.5 |
| `claude-haiku-5-5` | CHAT, VISION | Claude Haiku 5.5 快速版 |

### Google Gemini

| 模型 ID | 能力 | 说明 |
|---------|------|------|
| `gemini-2.0-flash` | CHAT, VISION | Gemini 2.0 Flash |
| `gemini-1.5-pro` | CHAT, VISION | Gemini 1.5 Pro |
| `gemini-1.5-flash` | CHAT, VISION | Gemini 1.5 Flash |

---

## 能力说明

| 能力 | 说明 | 请求特征 |
|------|------|----------|
| `CHAT` | 纯文本对话 | messages 中 content 为字符串 |
| `VISION` | 图像理解 | messages 中 content 包含 image_url |
| `IMAGE_GEN` | 图像生成 | 使用 `/v1/images/generations` 端点 |

---

## 路由规则

1. **指定模型**：请求中 `model` 字段指定具体模型 ID，直接路由到对应 Provider
2. **自动路由**：未指定模型时，根据能力从数据库配置中加权选择
3. **故障转移**：主 Provider 失败时，自动降级到备用 Provider

配置在数据库 `gateway_capability_route` 表中管理。

---

## 快速测试

### 使用 curl

```bash
# 简单聊天
curl -X POST http://localhost:8082/v1/chat/completions \
  -H "Authorization: Bearer gw-default-key-12345" \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-4o","messages":[{"role":"user","content":"Hello"}]}'
```

### 使用 Swagger UI

1. 访问 http://localhost:8082/swagger-ui.html
2. 点击 "Authorize" 按钮
3. 输入 `Bearer gw-default-key-12345`
4. 测试任意接口

---

*文档版本: 1.0.0*  
*更新日期: 2026-11-14*
