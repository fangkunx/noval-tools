# LangSmith 链路追踪集成文档

## 概述

网关已集成 LangSmith 规范的链路追踪系统，支持：
- 自动生成 traceId 和 spanId
- 记录每次 Provider 调用的完整链路
- 可选上报到 LangSmith 平台查看可视化 trace

## 配置

### application-dev.yml

```yaml
# LangSmith tracing configuration
langsmith:
  enabled: false  # 设置为 true 启用 LangSmith trace 上报
  api-key: ${LANGSMITH_API_KEY:}  # 你的 LangSMITH API Key
  endpoint: https://api.smith.langchain.com
  project-name: ai-gateway-dev

logging:
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] [%X{traceId}] [%X{spanId}] %-5level %logger{36} - %msg%n"
```

### 启用 LangSmith

1. 注册 LangSmith 账号：https://smith.langchain.com/
2. 获取 API Key
3. 设置环境变量或修改配置：
   ```bash
   export LANGSMITH_API_KEY=ls__your-api-key
   ```
4. 修改配置：
   ```yaml
   langsmith:
     enabled: true
   ```

## 日志格式

启用链路追踪后，日志将包含 traceId 和 spanId：

```
12:34:56.789 [http-nio-8082-exec-1] [abc123def456] [span789] INFO  provider.request - [Provider Request Start] provider=DEEPSEEK, model=deepseek-chat, messageCount=1, stream=false, traceId=abc123def456, body={...}
12:34:57.123 [http-nio-8082-exec-1] [abc123def456] [span789] INFO  provider.request - [Provider Request Success] provider=DEEPSEEK, model=deepseek-chat, duration=334ms, traceId=abc123def456, content=你好！
```

## LangSmith Run 结构

每次请求会生成以下 Run：

```
Request (chain)
  ├── traceId: abc123
  ├── Request Start
  │   ├── Provider: DEEPSEEK
  │   ├── Model: deepseek-chat
  │   └── Inputs: {messages, stream, ...}
  └── Provider Call (llm)
      ├── run_type: llm
      ├── inputs: {provider, model, message_count, stream}
      ├── outputs: {content}
      └── extra: {duration_ms, provider, model}
```

## LangSmith API 规范

### Run 对象

```json
{
  "id": "uuid",
  "trace_id": "uuid",
  "parent_run_id": "uuid" | null,
  "name": "DEEPSEEK Chat",
  "run_type": "llm",
  "start_time": "2024-01-01T00:00:00Z",
  "end_time": "2024-01-01T00:00:01Z",
  "inputs": {
    "provider": "DEEPSEEK",
    "model": "deepseek-chat",
    "message_count": 1,
    "stream": false
  },
  "outputs": {
    "content": "你好！"
  },
  "error": null,
  "extra": {
    "duration_ms": 334,
    "provider": "DEEPSEEK",
    "model": "deepseek-chat"
  },
  "session_id": "ai-gateway-dev"
}
```

### API Endpoint

- **POST** `https://api.smith.langchain.com/runs`
- **Headers**:
  - `x-api-key: <LANGSMITH_API_KEY>`
  - `Content-Type: application/json`

## 测试

### 1. 本地日志测试

启动服务后，调用 API 查看日志：

```bash
curl -X POST http://localhost:8082/v1/chat/completions \
  -H "Authorization: Bearer gw-default-key-12345" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d '{"model":"deepseek-chat","messages":[{"role":"user","content":"你好"}]}'
```

日志输出：

```
12:34:56.789 [http-nio-8082-exec-1] [abc123def456] [span789] INFO  provider.request - [Provider Request Start] provider=DEEPSEEK, model=deepseek-chat, messageCount=1, stream=false, traceId=abc123def456
12:34:57.123 [http-nio-8082-exec-1] [abc123def456] [span789] INFO  provider.request - [Provider Request Success] provider=DEEPSEEK, model=deepseek-chat, duration=334ms, traceId=abc123def456, content=你好！
```

### 2. LangSmith 平台测试

1. 启用 LangSmith（设置 `langsmith.enabled=true`）
2. 配置 API Key
3. 调用 API
4. 访问 https://smith.langchain.com/ 查看 trace

## 关键代码位置

| 文件 | 说明 |
|------|------|
| `TraceContext.java` | 链路上下文管理（traceId, spanId） |
| `TraceInterceptor.java` | HTTP 请求拦截器，初始化 trace |
| `ProviderRequestLogger.java` | Provider 请求日志记录器 |
| `LangSmithRun.java` | LangSmith Run 模型 |
| `LangSmithClient.java` | LangSmith API 客户端 |

## 链路追踪流程

```
1. HTTP 请求进入
   ↓
2. TraceInterceptor.preHandle()
   - 生成 traceId
   - 生成 spanId
   - 放入 MDC
   ↓
3. Controller 处理请求
   ↓
4. ChatService.chat()
   - 路由到 Provider
   ↓
5. Provider.chat()
   - logChatRequestStart() → 记录开始
   - 调用 API
   - logChatRequestSuccess/Error() → 记录结束
   ↓
6. TraceInterceptor.afterCompletion()
   - 记录响应状态
   - 清理 MDC
```

## 自定义扩展

### 添加新的 trace 字段

修改 `LangSmithRun.java`：

```java
@JsonProperty("custom_field")
private String customField;
```

### 添加新的 run_type

修改 `ProviderRequestLogger.java`：

```java
run = LangSmithRun.builder()
    .runType("custom_type")  // llm, chain, tool, retriever
    .build();
```

## 参考文档

- [LangSmith API 文档](https://docs.smith.langchain.com/reference/api)
- [LangSmith Trace 格式](https://docs.smith.langchain.com/tracing/faq/tracing_faq)
- [LangChain Python SDK](https://github.com/langchain-ai/langsmith-sdk)

---

**更新日期**: 2026-11-14
