---
name: txt2img-ddd-refactoring
description: txt2img module refactored to DDD four-layer architecture with SPI pattern for multi-model support
metadata:
  type: project
---

txt2img 模块已按 DDD 四层架构重构：

**架构**: `interfaces → application → domain ← infrastructure`

**核心变更**:
- 新增 `ImageProvider` SPI 接口（domain/spi/），所有 AI 后端实现此接口
- LK888 和 TokenPlan 各自实现为 `@Component`，Spring 通过 `List<ImageProvider>` 自动发现
- 模型定义从 Controller 硬编码移至各自 Provider 的 `getSupportedModels()`
- `ImageGenCommand`（命令）和 `ImageGenResult`（结果）使用 Builder 模式，不可变
- `Map<String,Object>` 全部替换为类型化对象（ErrorResponse, UsageInfo, ModelParam 等）
- Controller 中的 `if ("tokenplan".equals(x))` 字符串分支替换为 BackendType 枚举 + Provider 路由
- 前端 API 契约不变（端点路径、字段名完全兼容）

**扩展**: 新增后端只需添加一个 `@Component` 实现 `ImageProvider`，无需修改现有代码。

**规则文件**: `.claude/rules/txt2img-architecture.md`
**Why:** 原代码使用 Map/String[][] 传参、字符串硬编码分支、贫血 DTO，无法方便扩展新模型。
**How to apply:** 任何新增 txt2img 后端时，参考 [[txt2img-provider-extension]]；所有代码审查时检查是否符合此规范。