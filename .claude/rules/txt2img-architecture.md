# txt2img 模块架构规范

## DDD 四层架构

```
com.noval.txt2img
├── Txt2ImgApplication.java           [Spring Boot 入口，不修改]
│
├── interfaces/rest                   [接口层]
│   ├── ImageController.java          [REST Controller，薄层]
│   └── dto/
│       ├── GenerateRequest.java      [请求 DTO，含 toCommand() 转换]
│       ├── TaskStatusResponse.java   [响应 DTO，含 from(ImageGenResult) 工厂]
│       ├── ModelSpecResponse.java    [模型列表响应 DTO，options 转为管道分隔字符串]
│       └── ErrorResponse.java        [类型化错误响应，替代 Map]
│
├── application                       [应用层]
│   └── ImageGenService.java          [编排服务：路由 Provider、聚合模型列表]
│
├── domain                            [领域层 —— 不依赖任何外层]
│   ├── spi/
│   │   └── ImageProvider.java        [SPI 核心接口：所有 AI 后端的契约]
│   └── model/
│       ├── BackendType.java          [枚举：LK888, TOKENPLAN]
│       ├── ParamType.java            [枚举：SELECT, TEXT, NUMBER, IMAGES]
│       ├── ModelParam.java           [值对象：单个模型参数定义]
│       ├── ModelSpec.java            [值对象：模型规格声明]
│       ├── ImageGenCommand.java      [命令对象：不可变，Builder 模式]
│       └── ImageGenResult.java       [结果对象：不可变，Builder 模式，含 UsageInfo]
│
└── infrastructure                    [基础设施层 —— 实现 domain SPI]
    └── provider/
        ├── Lk888Provider.java        [lk888 后端实现 + 4 个模型声明]
        └── TokenPlanProvider.java    [百炼后端实现 + 3 个模型声明]
```

### 依赖方向
```
interfaces → application → domain ← infrastructure
```
- **domain** 层不依赖任何外层（零依赖原则）
- **application** 层依赖 domain，不依赖 infrastructure
- **infrastructure** 层实现 domain 的 SPI 接口
- **interfaces** 层依赖 application 和 domain

---

## 核心规则

### 1. 禁止使用 Map 作为参数或返回值
- 所有参数必须使用类型化对象（DTO / Command / Value Object）
- 所有返回值必须使用类型化对象（Result / Response）
- 错误响应使用 `ErrorResponse` 类，**禁止** `new HashMap<>()`
- 仅 `/api/models` 顶层信封使用 `Map`（前端要求的固定 `{type, models}` 结构），内部 models 是 typed `ModelSpecResponse` 列表

### 2. Provider 接口统一（SPI 策略模式）
- 所有 AI 后端必须实现 `ImageProvider` 接口（`domain/spi/ImageProvider.java`）
- 新增后端只需添加一个 `@Component` 实现类
- Spring 通过 `List<ImageProvider>` 自动发现所有提供者
- **禁止**在 Controller 中使用 `if ("tokenplan".equals(x))` 字符串匹配选择后端

### 3. 模型定义归属 Provider
- 每个 Provider 通过 `getSupportedModels()` 声明其支持的模型
- `ModelSpec` 和 `ModelParam` 是类型化、不可变的值对象
- **禁止**使用 `String[][]` 或 `Map<String, Object>` 定义模型参数
- 添加新模型 = 在对应 Provider 的 `MODELS` 列表中添加一个 `ModelSpec`

### 4. 命令/结果对象分离
- **Controller 层**：使用 DTO（Jackson 序列化，含 getter/setter）
- **Domain 层**：使用 Command/Result（纯业务对象，不可变，Builder 模式）
- **Application 层**：DTO ↔ Domain 转换
- `GenerateRequest.toCommand()` 完成 DTO → Command 转换
- `TaskStatusResponse.from(ImageGenResult)` 完成 Result → Response 转换

### 5. 不可变领域对象
- `ImageGenCommand` — Builder 模式，`prompt` 必填校验
- `ImageGenResult` — Builder 模式，含内嵌 `UsageInfo` 类型化对象（替代 `Map<String,Object>`）
- `ModelSpec`, `ModelParam` — 全参构造，仅 getter，`List` 字段防御性拷贝为 `Collections.unmodifiableList`

### 6. 前端兼容性约束
- API 端点不变：`POST /api/generate`, `GET /api/status/{taskId}`, `GET /api/models`
- 响应字段名不变：`state`, `error`, `taskId`, `isFinal`, `progress`, `status`, `cost`, `resultUrls`, `resultUrl`, `imageUrls`, `requestId`
- 模型参数 `options` 为管道分隔字符串（`"1K|2K|4K"`），而非 JSON 数组
- `isFinal` 序列化为 `"isFinal"`（通过 `getIsFinal()` getter）

---

## 扩展新后端步骤
1. 在 `BackendType` 枚举添加新值
2. 创建新类实现 `ImageProvider` 接口
3. 添加 `@Component` 注解（Spring 自动发现）
4. 实现 `getBackend()`, `getSupportedModels()`, `generate()`, `poll()`, `supportsAsync()`
5. 在 `application.yml` 添加对应的 API key 配置
6. **无需修改任何现有代码**（开闭原则）

---

## 与 novel-to-comic 模块的区别
- novel-to-comic 使用传统分层（Controller → Service），无 DDD 分层
- txt2img 使用 DDD 四层，核心区别：
  - domain 层零外部依赖
  - SPI 接口实现策略模式
  - 不可变领域对象替代贫血模型
  - Provider 自动发现替代硬编码分支