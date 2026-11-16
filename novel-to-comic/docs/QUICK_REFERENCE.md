# 快速参考卡片

> 常用规范速查，建议打印贴在工位。

---

## 📝 命名速查

| 类型 | 规则 | 示例 |
|------|------|------|
| 包名 | 全小写，域名倒序 | `com.novel.comic.service` |
| 类名 | UpperCamelCase | `PipelineController` |
| 方法名 | lowerCamelCase | `getTaskById()` |
| 变量名 | lowerCamelCase | `currentTaskId` |
| 常量 | UPPER_SNAKE_CASE | `MAX_TOKENS` |
| 布尔变量 | is/has/can 前缀 | `isRunning`, `hasResult` |

---

## 🏗️ 分层职责

| 层 | 职责 | 禁止 |
|----|------|------|
| Controller | HTTP 请求/响应 | 业务逻辑、直接操作 DB |
| Service | 核心业务逻辑 | 依赖 Spring MVC |
| Util | 纯工具方法 | 持有状态、依赖 Bean |

---

## 🗄️ 数据库命名

```
novel_tasks          ← 模块_实体（小写下划线）
step_status          ← 步骤状态表
step_split           ← 拆分数据表
idx_split_task       ← idx_表名_字段（索引）
```

---

## 🔌 API 设计

```
GET    /api/projects          # 列表
GET    /api/projects/{id}     # 详情
POST   /api/init              # 创建
POST   /api/step/{idx}        # 执行
DELETE /api/projects/{id}     # 删除
```

---

## 🎨 前端规范

```javascript
// ✅ 变量声明
const State = { ... };
let currentStep = 0;

// ✅ 函数命名
function initNavigation() { }
function loadProject(id) { }

// ✅ DOM 查询
const $ = s => document.querySelector(s);
const $$ = s => document.querySelectorAll(s);
```

```css
/* ✅ 使用变量 */
:root {
  --accent: #1a5fd0;
  --r-md: 6px;
}

.card {
  background: var(--card);
  border-radius: var(--r-lg);
}
```

---

## 📋 Commit 格式

```
type(scope): subject

feat(ai): 接入 DeepSeek Flash
fix(ui): 修复步骤面板显示
docs(readme): 更新安装说明
refactor(service): 重构状态管理
```

**Type**：
- `feat` - 新功能
- `fix` - 修复
- `docs` - 文档
- `style` - 格式
- `refactor` - 重构
- `test` - 测试
- `chore` - 构建

---

## ⚙️ 配置示例

```yaml
# application-dev.yml
app:
  provider: "deepseek"          # anthropic / deepseek
  api-key: "sk-xxx"             # 从环境变量或 dev 文件读取
  base-url: "https://api.deepseek.com"
  model: "deepseek-chat"
  max-tokens: 4096
  temperature: 0.7
  default-segments: 90
```

---

## 🎯 新增步骤流程

1. 创建 `XxxStepStrategy.java`
2. 实现 `StepStrategy` 接口
3. 在 `StepStrategyRegistry` 注册
4. 在 `PipelineService.STEP_DEFS` 添加定义
5. 在 `TaskStore` 添加数据表（如需要）
6. 更新前端步骤显示

---

## 🚨 常见错误

| 错误 | 正确做法 |
|------|----------|
| `var x = 1` | `const x = 1` 或 `let x = 1` |
| `if(list.size()==0)` | `if(list.isEmpty())` |
| `catch(Exception e){}` | `catch(SQLException e){ log.error(...) }` |
| `System.out.println()` | `log.info()` |
| SQL 字符串拼接 | `PreparedStatement` |
| `@Autowired` 字段 | 构造函数注入 |

---

## 📚 文档位置

- **开发规范**：`docs/DEVELOPMENT_STANDARDS.md`
- **代码审查**：`docs/CODE_REVIEW_CHECKLIST.md`
- **架构设计**：`docs/DESIGN_DOC.md`
- **原型图**：`docs/prototype-v2.html`

---

**最后更新**：2026-11-16
