# AI Gateway 模块

统一 AI 能力代理层，兼容 OpenAI 协议，支持多种 AI 后端。

## 功能特性

- **OpenAI 兼容协议**：对外暴露 `/v1/chat/completions`, `/v1/images/generations`, `/v1/models`
- **多 Provider 支持**：OpenAI、Claude、Gemini
- **能力路由**：CHAT / VISION / IMAGE_GEN
- **加权负载均衡**：数据库配置权重和优先级
- **自动故障转移**：主 Provider 失败自动降级到备用
- **流式响应**：SSE 流式输出
- **API Key 认证**：Bearer Token 验证
- **Swagger 文档**：在线 API 文档和测试

## 快速开始

### 1. 配置 API Key

编辑 `application.yml` 或设置环境变量：

```yaml
gateway:
  providers:
    openai:
      api-key: ${OPENAI_API_KEY:sk-xxx}
    claude:
      api-key: ${CLAUDE_API_KEY:sk-ant-xxx}
    google:
      api-key: ${GOOGLE_API_KEY:xxx}
```

### 2. 启动服务

```bash
mvn spring-boot:run -pl gateway
```

服务默认运行在 `http://localhost:8082`

### 3. 访问 Swagger UI

打开浏览器访问：http://localhost:8082/swagger-ui.html

### 4. 调用示例

#### 聊天（需要认证）

```bash
curl -X POST http://localhost:8082/v1/chat/completions \
  -H "Authorization: Bearer gw-default-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

#### 视觉理解（带图片）

```bash
curl -X POST http://localhost:8082/v1/chat/completions \
  -H "Authorization: Bearer gw-default-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{
      "role": "user",
      "content": [
        {"type": "text", "text": "描述这张图片"},
        {"type": "image_url", "image_url": {"url": "https://example.com/image.jpg"}}
      ]
    }]
  }'
```

#### 图像生成

```bash
curl -X POST http://localhost:8082/v1/images/generations \
  -H "Authorization: Bearer gw-default-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "dall-e-3",
    "prompt": "A cute cat playing with yarn",
    "n": 1,
    "size": "1024x1024"
  }'
```

#### 获取模型列表

```bash
curl http://localhost:8082/v1/models \
  -H "Authorization: Bearer gw-default-key-12345"
```

#### 按能力过滤模型

```bash
# 获取支持视觉的模型
curl "http://localhost:8082/v1/models?capability=VISION" \
  -H "Authorization: Bearer gw-default-key-12345"
```

## 默认 API Key

系统初始化时会创建默认 API Key：`gw-default-key-12345`

可以在 H2 Console 中管理：http://localhost:8082/h2-console

## 架构

```
gateway/
├── domain/                    # 领域层（零依赖）
│   ├── model/                 # 领域模型
│   └── spi/                   # SPI 接口
├── application/               # 应用层（编排服务）
├── infrastructure/            # 基础设施层
│   ├── provider/              # Provider 实现
│   ├── persistence/           # JPA 持久化
│   └── config/                # 配置
└── interfaces/rest/           # 接口层（Controller + DTO）
```

## 扩展新 Provider

1. 实现 `GatewayProvider` 接口
2. 在 `ProviderBeanConfig` 注册 Bean
3. 在 `application.yml` 添加配置

```java
public class CustomProvider implements GatewayProvider {
    @Override
    public ProviderType getProviderType() { ... }
    
    @Override
    public ChatResult chat(ChatCommand command, String modelName) { ... }
    
    // ... 其他方法
}
```

## 运行测试

```bash
mvn test -pl gateway
```

## 技术栈

- Spring Boot 2.7
- Java 8
- SpringDoc OpenAPI 1.7
- H2 Database
- JPA/Hibernate
