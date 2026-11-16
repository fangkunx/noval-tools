# Novel-to-Comic 项目开发规范

> 基于《阿里巴巴 Java 开发手册》泰山版，结合本项目实际情况制定。
> **所有开发必须严格遵守本规范。**

---

## 1. 项目结构规范

### 1.1 目录结构

```
novel-to-comic/
├── src/main/java/com/novel/comic/
│   ├── NovelToComicApplication.java    # 启动类
│   ├── config/                         # 配置类
│   │   └── AppConfig.java
│   ├── controller/                     # 控制器层
│   │   └── PipelineController.java
│   ├── service/                        # 服务层
│   │   ├── AiService.java
│   │   ├── PipelineService.java
│   │   ├── SseService.java
│   │   ├── TaskStore.java
│   │   └── step/                       # 策略模式 - 步骤实现
│   │       ├── StepStrategy.java       # 策略接口
│   │       ├── StepContext.java        # 执行上下文
│   │       ├── StepStrategyRegistry.java
│   │       ├── SplitStepStrategy.java
│   │       ├── CharactersStepStrategy.java
│   │       ├── TurnaroundStepStrategy.java
│   │       ├── StoryboardStepStrategy.java
│   │       ├── RenameStepStrategy.java
│   │       ├── PromptsStepStrategy.java
│   │       └── LastPageStepStrategy.java
│   └── util/                           # 工具类
│       └── Maps.java
├── src/main/resources/
│   ├── application.yml                 # 主配置
│   ├── application-dev.yml             # 开发环境配置（gitignored）
│   ├── static/                         # 前端静态文件
│   │   ├── index.html
│   │   ├── style.css
│   │   └── app.js
│   └── prompts/                        # AI 提示词模板
│       ├── split.txt
│       ├── characters.txt
│       ├── character-turnaround.txt
│       ├── storyboard.txt
│       └── style-*.txt                 # 画风模板
├── docs/                               # 文档目录
├── prompts/                            # 运行时提示词目录
├── pom.xml
└── pipeline.db                         # SQLite 数据库（运行时生成）
```

### 1.2 分层依赖规则

```
controller → service → util
     ↓          ↓
    (HTTP)    (业务逻辑)
```

- **Controller 层**：仅处理 HTTP 请求/响应，不含业务逻辑
- **Service 层**：核心业务逻辑，不依赖 Spring MVC
- **Util 层**：纯工具类，无状态，无依赖

### 1.3 禁止事项

- ❌ Controller 中直接操作数据库
- ❌ Service 中直接操作 HttpServletRequest/Response
- ❌ Util 类中持有状态或依赖 Spring Bean
- ❌ 在循环中调用数据库（应批量操作）

---

## 2. 命名规范

### 2.1 包命名

- **规则**：全部小写，域名倒序 + 模块名
- **格式**：`com.novel.comic.{模块}`
- **示例**：
  - `com.novel.comic.config`
  - `com.novel.comic.controller`
  - `com.novel.comic.service`
  - `com.novel.comic.service.step`
  - `com.novel.comic.util`

### 2.2 类命名

- **规则**：UpperCamelCase，名词或名词短语
- **后缀约定**：
  - `Controller`：控制器
  - `Service`：服务接口/实现
  - `Strategy`：策略实现
  - `Config`：配置类
  - `Store`：数据存储
  - `Exception`：自定义异常
  - `DTO`/`VO`：数据传输对象
  - `Enum`：枚举类

- **示例**：
  ```java
  // ✅ 正确
  public class PipelineController { }
  public class AiService { }
  public class StepStrategyRegistry { }
  public class AppConfig { }
  public class TaskStore { }
  
  // ❌ 错误
  public class pipelineController { }      // 首字母小写
  public class AI_service { }              // 下划线
  public class Manager { }                 // 含义模糊
  ```

### 2.3 方法命名

- **规则**：lowerCamelCase，动词或动词短语
- **前缀约定**：
  - `get`：获取单个对象
  - `list`：获取列表
  - `count`：获取数量
  - `is`/`has`/`can`：布尔判断
  - `create`/`add`/`save`：新增
  - `update`/`modify`：修改
  - `delete`/`remove`：删除
  - `load`：从存储加载
  - `build`：构建对象
  - `execute`/`run`：执行操作

- **示例**：
  ```java
  // ✅ 正确
  public Task getTask(String id) { }
  public List<Task> listTasks() { }
  public boolean isRunning(int idx) { }
  public String createTask(String title) { }
  public void updateTaskMeta(String id) { }
  public Map<String, Object> buildStatusResponse() { }
  
  // ❌ 错误
  public Task task(String id) { }          // 缺少动词
  public List<Task> getTaskList() { }      // 应使用 listTasks
  public void process() { }                // 含义模糊
  ```

### 2.4 变量命名

- **规则**：lowerCamelCase，语义清晰
- **常量**：全部大写，下划线分隔
- **布尔变量**：`is`/`has`/`can`/`should` 前缀

- **示例**：
  ```java
  // ✅ 正确
  private String currentTaskId;
  private List<String> segments;
  private boolean isRunning;
  private static final int MAX_TOKENS = 4096;
  
  // ❌ 错误
  private String id;                       // 含义不清
  private List<String> s;                  // 缩写
  private boolean runningFlag;             // 冗余后缀
  private static final int maxTokens = 4096; // 常量未大写
  ```

### 2.5 枚举命名

- **枚举类**：UpperCamelCase
- **枚举值**：全部大写，下划线分隔

- **示例**：
  ```java
  public enum TaskStatus {
      CREATED,
      IN_PROGRESS,
      COMPLETED,
      FAILED
  }
  ```

---

## 3. 编码规范

### 3.1 OOP 规范

- **避免通过构造函数传递多个参数**，使用 Builder 模式
- **所有成员变量必须 private**，通过 getter/setter 访问
- **工具类必须私有构造函数**，防止实例化

```java
// ✅ 正确
public final class Maps {
    private Maps() { throw new UnsupportedOperationException(); }
    
    @SafeVarargs
    public static Map<String, String> of(String... kvs) { ... }
}

// ❌ 错误
public class Maps {
    // 缺少私有构造函数
}
```

### 3.2 集合规范

- **判断集合为空**：使用 `Collection.isEmpty()` 而非 `size() == 0`
- **集合泛型**：必须指定，不使用原始类型
- **并发集合**：使用 `ConcurrentHashMap.newKeySet()` 而非 `Collections.synchronizedSet()`

```java
// ✅ 正确
if (characters.isEmpty()) { ... }
private final List<String> segments = new ArrayList<>();
private final Set<Integer> running = ConcurrentHashMap.newKeySet();

// ❌ 错误
if (characters.size() == 0) { ... }
private List segments = new ArrayList();
```

### 3.3 并发规范

- **共享变量**：使用 `volatile` 或 `AtomicXxx`
- **线程安全集合**：使用 `ConcurrentHashMap`、`CopyOnWriteArrayList`
- **锁**：使用 `ReentrantReadWriteLock` 区分读写锁

```java
// ✅ 正确
private volatile String currentTaskId;
private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

public void updateTask() {
    lock.writeLock().lock();
    try {
        // 写操作
    } finally {
        lock.writeLock().unlock();
    }
}

// ❌ 错误
private String currentTaskId;  // 多线程访问未加 volatile
```

### 3.4 异常规范

- **不要捕获 `Exception` 或 `Throwable`**，应捕获具体异常
- **异常必须处理**，不能吞掉（至少打日志）
- **业务异常**：使用 `RuntimeException` 并附带清晰消息

```java
// ✅ 正确
try {
    // ...
} catch (SQLException e) {
    log.error("Failed to load task {}", taskId, e);
    return null;
}

throw new RuntimeException("请先执行角色提取");

// ❌ 错误
try {
    // ...
} catch (Exception e) {
    // 吞掉异常
}
```

### 3.5 日志规范

- **使用 SLF4J**，不使用 `System.out.println`
- **日志级别**：ERROR（错误）、WARN（警告）、INFO（关键流程）、DEBUG（调试）
- **占位符**：使用 `{}` 而非字符串拼接

```java
// ✅ 正确
private static final Logger log = LoggerFactory.getLogger(AiService.class);
log.info("Created task [{}] \"{}\" ({} chars)", taskId, title, novelText.length());
log.error("Failed to load task {}", taskId, e);

// ❌ 错误
System.out.println("Task created: " + taskId);
log.info("Task created: " + taskId);
```

---

## 4. 架构规范

### 4.1 策略模式

所有步骤必须实现 `StepStrategy` 接口：

```java
@FunctionalInterface
public interface StepStrategy {
    void execute(StepContext ctx) throws Exception;
}
```

**新增步骤流程**：
1. 在 `StepStrategyRegistry` 构造函数中注册
2. 创建 `XxxStepStrategy.java` 实现类
3. 在 `PipelineService.STEP_DEFS` 中添加定义
4. 在 `TaskStore` 中添加对应的数据表（如需要）

### 4.2 依赖注入

- **使用构造函数注入**，不使用 `@Autowired` 字段注入
- **所有 Spring Bean 必须显式声明依赖**

```java
// ✅ 正确
public PipelineService(
    AppConfig.PipelineConfig config,
    AiService aiService,
    SseService sseService,
    ObjectMapper mapper,
    StepStrategyRegistry strategyRegistry,
    TaskStore taskStore
) {
    this.config = config;
    this.aiService = aiService;
    // ...
}

// ❌ 错误
@Autowired
private AiService aiService;
```

### 4.3 状态管理

- **PipelineService**：管理运行时状态（内存）
- **TaskStore**：管理持久化状态（SQLite）
- **同步规则**：每次步骤完成后调用 `saveState()`

```java
public void finishStep(int idx) {
    steps.get(idx).status = "done";
    saveState();  // 必须调用
}
```

---

## 5. 数据库规范

### 5.1 表命名

- **规则**：小写，下划线分隔，模块前缀
- **格式**：`{模块}_{实体}`

```sql
-- ✅ 正确
CREATE TABLE novel_tasks ( ... );
CREATE TABLE step_status ( ... );
CREATE TABLE step_split ( ... );
CREATE TABLE step_characters ( ... );

-- ❌ 错误
CREATE TABLE Tasks ( ... );           -- 大写
CREATE TABLE stepStatus ( ... );      -- 驼峰
CREATE TABLE split ( ... );           -- 缺少模块前缀
```

### 5.2 字段命名

- **规则**：小写，下划线分隔
- **主键**：统一使用 `id`
- **时间字段**：`created_at`、`updated_at`
- **外键**：`{实体}_id`

```sql
-- ✅ 正确
id              TEXT PRIMARY KEY,
task_id         TEXT NOT NULL REFERENCES novel_tasks(id),
step_index      INTEGER NOT NULL,
status          TEXT NOT NULL DEFAULT 'pending',
created_at      TEXT NOT NULL,
updated_at      TEXT
```

### 5.3 索引规范

- **外键必须建索引**
- **索引命名**：`idx_{表名}_{字段}`

```sql
-- ✅ 正确
CREATE INDEX IF NOT EXISTS idx_split_task ON step_split(task_id);
CREATE INDEX IF NOT EXISTS idx_chars_task ON step_characters(task_id);
```

### 5.4 SQL 规范

- **使用 PreparedStatement**，禁止字符串拼接 SQL
- **批量操作**：使用事务包裹

```java
// ✅ 正确
PreparedStatement ps = c.prepareStatement(
    "INSERT INTO step_split (task_id, segment_index, content) VALUES (?,?,?)"
);
ps.setString(1, taskId);
ps.setInt(2, i);
ps.setString(3, segments.get(i));

// ❌ 错误
stmt.execute("INSERT INTO step_split VALUES ('" + taskId + "', " + i + ", '" + content + "')");
```

---

## 6. API 设计规范

### 6.1 RESTful 规范

- **URL**：小写，名词复数，使用 `/api/{resource}/{id}`
- **HTTP 方法**：GET（查询）、POST（创建）、PUT（更新）、DELETE（删除）
- **响应格式**：统一 JSON，包含 `success`/`error` 字段

```java
// ✅ 正确
GET    /api/status                    // 获取状态
GET    /api/projects                  // 获取项目列表
POST   /api/init                      // 初始化任务
POST   /api/step/{idx}                // 执行步骤
DELETE /api/projects/{id}             // 删除项目

// ❌ 错误
GET    /api/getStatus                 // 动词
POST   /api/createProject             // 动词
GET    /api/project?id=123            // 应使用 /api/projects/123
```

### 6.2 响应格式

```json
// 成功响应
{
  "success": true,
  "taskId": "abc123",
  "steps": [ ... ]
}

// 错误响应
{
  "error": "小说文本不能为空"
}

// 状态响应
{
  "taskId": "abc123",
  "hasState": true,
  "title": "未命名作品",
  "steps": [
    { "index": 0, "id": "split", "name": "拆分", "status": "done" }
  ]
}
```

### 6.3 SSE 事件规范

- **事件类型**：`chunk`、`progress`、`status`、`result`、`done`、`error`
- **数据格式**：JSON

```java
// ✅ 正确
ctx.emit("progress", Maps.of("message", "处理中...", "done", 5, "total", 10));
ctx.emit("chunk", Maps.of("text", "新增文本", "index", 0));

// ❌ 错误
sseService.broadcast(idx, "data", "纯文本");  // 非 JSON
```

---

## 7. 前端规范

### 7.1 文件组织

```
static/
├── index.html    # HTML 结构
├── style.css     # CSS 样式
└── app.js        # JavaScript 逻辑
```

### 7.2 CSS 规范

- **使用 CSS 变量**：所有颜色、尺寸使用 `--variable`
- **命名**：lower-kebab-case（短横线分隔）
- **选择器**：优先使用 class，避免使用 id

```css
/* ✅ 正确 */
:root {
  --accent: #1a5fd0;
  --r-md: 6px;
  --fs-14: 14px;
}

.project-card {
  background: var(--card);
  border-radius: var(--r-lg);
}

/* ❌ 错误 */
#projectCard {
  background: #fff;
  border-radius: 8px;
}
```

### 7.3 JavaScript 规范

- **变量声明**：使用 `const`/`let`，禁止使用 `var`
- **函数命名**：lowerCamelCase
- **事件绑定**：统一在 `DOMContentLoaded` 中初始化
- **DOM 查询**：使用 `$`/`$$` 简写

```javascript
// ✅ 正确
const State = {
  currentTask: { id: null, title: '' },
  ui: { isBusy: false }
};

function initNavigation() {
  $$('.nav-btn').forEach(btn =>
    btn.addEventListener('click', () => switchScreen(btn.dataset.screen))
  );
}

// ❌ 错误
var state = { ... };
function InitNavigation() { ... }
document.getElementById('btn').onclick = function() { ... };
```

### 7.4 状态管理规范

- **全局状态**：统一使用 `State` 对象
- **UI 状态**：`State.ui.xxx`
- **任务状态**：`State.currentTask.xxx`
- **项目列表**：`State.projects[]`

```javascript
// ✅ 正确
const State = {
  currentScreen: 'home',
  projects: [],
  currentTask: { id: null, title: '', steps: [] },
  ui: { isBusy: false, selectedStep: -1 }
};

// ❌ 错误
let currentScreen = 'home';
let projects = [];
let isBusy = false;
```

---

## 8. 配置规范

### 8.1 配置文件分层

- `application.yml`：主配置（默认值、生产环境）
- `application-dev.yml`：开发环境配置（gitignored）
- `application-prod.yml`：生产环境配置（gitignored）

### 8.2 配置项命名

- **规则**：lower-kebab-case
- **格式**：`app.{模块}.{配置项}`

```yaml
# ✅ 正确
app:
  provider: "deepseek"
  api-key: "sk-xxx"
  base-url: "https://api.deepseek.com"
  model: "deepseek-chat"
  max-tokens: 4096
  default-segments: 90

# ❌ 错误
app:
  providerName: "deepseek"      # 驼峰
  apiKey: "sk-xxx"              # 驼峰
```

### 8.3 敏感信息

- **禁止**：API Key、密码明文提交到 Git
- **使用**：`application-dev.yml` 存储开发环境密钥（已 gitignore）

---

## 9. 提示词模板规范

### 9.1 文件命名

- **基础模板**：`{步骤名}.txt`
- **画风模板**：`{步骤名}-{画风}.txt`

```
prompts/
├── split.txt                    # 文本拆分（基础）
├── characters.txt               # 角色提取（基础）
├── character-turnaround.txt     # 角色三视图（基础）
├── storyboard.txt               # 分镜（基础）
├── style-korean.txt             # 韩画画风
├── style-urban.txt              # 清冷都市
└── style-chibi.txt              # Q 版
```

### 9.2 变量替换

- **格式**：`{{VARIABLE_NAME}}`
- **全大写，下划线分隔**

```txt
角色列表：
{{CHARACTER_LIST}}

小说内容：
{{NOVEL_TEXT}}
```

### 9.3 新增画风流程

1. 创建 `prompts/style-{name}.txt`
2. 在 `AppConfig.PipelineConfig` 构造函数中添加映射
3. 在 `AiService.loadPromptForStyle()` 中添加判断

```java
// AppConfig.java
public PipelineConfig() {
    styleTemplates.put("韩漫", "style-korean");
    styleTemplates.put("Q 版", "style-chibi");
    styleTemplates.put("新画风", "style-new");  // 新增
}

// AiService.java
if ("新画风".equals(style)) styleName = "new";
```

---

## 10. Git 规范

### 10.1 分支命名

- `main`：主分支
- `feature/{功能名}`：功能分支
- `fix/{问题描述}`：修复分支
- `release/{版本号}`：发布分支

### 10.2 Commit 规范

**格式**：`type(scope): subject`

- `type`：feat（新功能）、fix（修复）、docs（文档）、style（格式）、refactor（重构）、test（测试）、chore（构建）
- `scope`：可选，影响的模块
- `subject`：简要描述（不超过 50 字）

```bash
# ✅ 正确
feat(ai): 接入 DeepSeek Flash 支持
fix(ui): 修复步骤面板显示异常
docs(readme): 更新安装说明
refactor(service): 重构 PipelineService 状态管理

# ❌ 错误
update code
fix bug
修改了一些东西
```

### 10.3 提交检查清单

- [ ] 代码已通过本地测试
- [ ] 无调试代码（`console.log`、`System.out.println`）
- [ ] 无敏感信息（API Key、密码）
- [ ] Commit 信息符合规范
- [ ] 已更新相关文档（如需要）

---

## 11. 测试规范

### 11.1 单元测试

- **命名**：`{方法名}_{场景}_{预期结果}`
- **工具**：JUnit 5 + Mockito

```java
@Test
void loadTask_TaskExists_ReturnsTask() {
    // Given
    String taskId = "test123";
    
    // When
    Map<String, Object> task = taskStore.loadTask(taskId);
    
    // Then
    assertNotNull(task);
    assertEquals("test123", task.get("id"));
}
```

### 11.2 集成测试

- 测试完整流程：初始化 → 执行步骤 → 获取结果
- 使用 H2 内存数据库替代 SQLite

---

## 12. 性能规范

### 12.1 数据库

- **批量操作**：使用事务包裹
- **索引**：外键字段必须建索引
- **避免 N+1 查询**：使用批量加载

### 12.2 内存

- **大文本**：使用 `StringBuilder` 拼接
- **集合**：预估大小，避免频繁扩容

### 12.3 网络

- **HTTP 客户端**：复用 `OkHttpClient` 实例
- **超时设置**：连接 30s、读取 300s（AI 调用）

---

## 13. 安全规范

### 13.1 敏感信息

- **API Key**：存储在 `application-dev.yml`，不提交 Git
- **用户输入**：必须校验和转义

### 13.2 SQL 注入

- **必须使用 PreparedStatement**
- **禁止字符串拼接 SQL**

### 13.3 XSS 防护

- **前端**：用户输入使用 `textContent` 而非 `innerHTML`
- **后端**：返回数据不直接拼接 HTML

---

## 14. 附录

### 14.1 常用命令

```bash
# 启动项目
mvn spring-boot:run

# 打包
mvn clean package

# 运行测试
mvn test

# 查看依赖
mvn dependency:tree
```

### 14.2 开发工具推荐

- **IDE**：IntelliJ IDEA
- **数据库**：DB Browser for SQLite
- **API 测试**：Postman / curl
- **前端调试**：Chrome DevTools

### 14.3 参考文档

- 《阿里巴巴 Java 开发手册》泰山版
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [DeepSeek API 文档](https://api-docs.deepseek.com/)

---

**最后更新**：2026-11-16
**版本**：v1.0
**维护者**：开发团队
