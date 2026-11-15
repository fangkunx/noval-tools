# 项目架构 Wiki

## 整体架构

Novel AI Tools 采用多模块 Maven 项目结构，各模块职责清晰，通过 AI 网关统一对外提供服务。

```
┌─────────────────────────────────────────────────────────┐
│                      前端应用层                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ 小说转漫画UI │  │  图片合成UI  │  │  文生图UI    │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
└─────────┼─────────────────┼─────────────────┼──────────┘
          │                 │                 │
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────┐
│                    AI Gateway 层                         │
│  ┌────────────────────────────────────────────────────┐ │
│  │  OpenAI 兼容 API (/v1/chat, /v1/images, etc.)     │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────┐ │
│  │  路由层：能力路由 + 负载均衡 + 故障转移            │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
          │                 │                 │
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────┐
│                   AI Provider 层                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │ OpenAI   │ │ Claude   │ │ Gemini   │ │ LK888    │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │ Grsai    │ │ KL Chat  │ │ KL Image │ │  ...     │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
└─────────────────────────────────────────────────────────┘
```

## 模块依赖关系

```
gateway (核心模块)
    ├── 提供统一的 AI 能力接口
    ├── 集成多个 AI Provider
    └── 对外暴露 REST API

novel-to-comic (业务模块)
    ├── 依赖 gateway 的 API
    ├── 实现小说转漫画流水线
    └── 提供项目管理功能

txt2img (独立模块)
    ├── DDD 架构，独立运行
    ├── 支持多后端切换
    └── 可作为示例参考
```

## 核心设计模式

### 1. SPI 策略模式

通过 `GatewayProvider` 接口定义统一契约，各 Provider 实现具体逻辑：

```java
public interface GatewayProvider {
    ProviderType getProviderType();
    List<ModelSpec> getSupportedModels();
    Set<Capability> getSupportedCapabilities();
    ChatResult chat(ChatCommand command, String modelName);
    ImageGenResult imageGen(ImageGenCommand command, String modelName);
    ImageGenResult imageEdit(ImageEditCommand command, String modelName);
    ImageGenResult imageCompose(ImageComposeCommand command, String modelName);
}
```

**优势**：
- 新增 Provider 无需修改现有代码（开闭原则）
- Spring 自动发现所有 `@Component` 实现
- 运行时动态路由到不同后端

### 2. DDD 四层架构

```
┌─────────────────────────────────────┐
│  interfaces (接口层)                │
│  - REST Controller                  │
│  - DTO (Request/Response)           │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  application (应用层)               │
│  - 编排服务 (Service)               │
│  - 协调领域对象                     │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  domain (领域层)                    │
│  - 实体、值对象                     │
│  - 领域服务                         │
│  - 零外部依赖                       │
└──────────────▲──────────────────────┘
               │
┌──────────────┴──────────────────────┐
│  infrastructure (基础设施层)        │
│  - SPI 实现                         │
│  - 持久化                           │
│  - 外部服务集成                     │
└─────────────────────────────────────┘
```

**依赖方向**：`interfaces → application → domain ← infrastructure`

### 3. 不可变对象模式

领域层使用不可变对象（Immutable Objects）：

```java
public class ImageGenCommand {
    private final String model;
    private final String prompt;
    private final Integer n;
    private final String size;
    
    private ImageGenCommand(Builder builder) {
        this.model = builder.model;
        this.prompt = builder.prompt;
        // ...
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
```

**优势**：
- 线程安全
- 易于测试
- 避免副作用

### 4. Builder 模式

复杂对象使用 Builder 模式创建：

```java
ImageGenCommand command = ImageGenCommand.builder()
    .model("tt-image-2.5")
    .prompt("a cute cat")
    .n(1)
    .size("1024x1024")
    .build();
```

## 数据流

### 图像生成流程

```
1. HTTP Request (JSON)
   ↓
2. Controller (DTO → Command 转换)
   ↓
3. Application Service (路由选择)
   ↓
4. Router Service (根据能力选择 Provider)
   ↓
5. Provider (调用外部 API)
   ↓
6. Result (Command → Response 转换)
   ↓
7. HTTP Response (JSON)
```

### 图片合成流程

```
1. 前端上传图片到七牛云 OSS
   ↓
2. 获取公网 URL
   ↓
3. 调用 /v1/images/compose API
   ↓
4. Gateway 路由到 LK888 Provider
   ↓
5. LK888 下载图片并合成
   ↓
6. 返回合成结果 URL
   ↓
7. 前端展示和下载
```

## 扩展指南

### 新增 AI Provider

1. 在 `domain/model/ProviderType.java` 添加枚举值
2. 创建新的 Provider 类实现 `GatewayProvider` 接口
3. 添加 `@Component` 注解
4. 在 `application.yml` 添加配置

```java
@Component
public class NewProvider implements GatewayProvider {
    @Override
    public ProviderType getProviderType() {
        return ProviderType.NEW_PROVIDER;
    }
    
    @Override
    public List<ModelSpec> getSupportedModels() {
        return Arrays.asList(
            new ModelSpec("model-name", "Model Name", 
                ProviderType.NEW_PROVIDER, 
                EnumSet.of(Capability.IMAGE_GEN))
        );
    }
    
    // ... 实现其他方法
}
```

### 新增能力类型

1. 在 `domain/model/Capability.java` 添加枚举值
2. 在 Provider 中实现对应方法
3. 在 RouterService 中添加路由规则

## 性能优化

### 1. 异步处理

图像生成采用异步模式：
- 提交任务返回 task_id
- 轮询获取结果
- 避免长连接超时

### 2. 缓存策略

- 模型列表缓存（避免重复查询）
- 路由配置缓存（数据库配置）

### 3. 并发控制

- 批量合成支持多任务并发
- 前端 Promise.all 并发处理
- 后端线程池管理

## 监控和日志

### LangSmith 集成

```yaml
langsmith:
  enabled: true
  api-key: ${LANGSMITH_API_KEY:}
  endpoint: https://api.smith.langchain.com
  project-name: ai-gateway
```

支持：
- 请求追踪
- 性能监控
- 错误分析

### 日志记录

```java
ProviderRequestLogger.logImageComposeRequestStart(
    provider, model, command, reqJson
);
```

记录：
- 请求参数
- 响应时间
- 成功/失败状态
- 完整请求 JSON

## 安全最佳实践

### 1. API Key 管理

```yaml
# 使用环境变量
gateway:
  providers:
    openai:
      api-key: ${OPENAI_API_KEY:}
```

### 2. 认证机制

```java
// Bearer Token 认证
Authorization: Bearer gw-default-key-12345
```

### 3. 敏感信息保护

```gitignore
# .gitignore
application-dev.yml
application-local.yml
*.log
data/
```

## 故障排查

### 常见问题

1. **Provider 调用失败**
   - 检查 API Key 是否正确配置
   - 检查网络连接
   - 查看日志中的错误信息

2. **路由不生效**
   - 检查数据库中的路由配置
   - 确认 Capability 是否匹配
   - 重启应用刷新缓存

3. **图片合成失败**
   - 检查七牛云配置
   - 确认图片 URL 可公网访问
   - 查看 LK888 API 返回的错误信息

### 调试技巧

1. 启用 DEBUG 日志
2. 使用 Swagger UI 测试 API
3. 查看 LangSmith 追踪
4. 检查数据库中的配置

---

更多详细文档请参考各模块的 README 和 `doc/` 目录下的文档。
