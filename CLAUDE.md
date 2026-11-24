# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

`noval-tools`：AI 小说 → 漫画工具集，多模块 Maven 工程（Spring Boot 2.7.18 / Java 8）。

| 模块 | 默认端口 | 职责 | 分层风格 |
|---|---|---|---|
| `gateway` | 8082 | OpenAI 兼容 AI 网关，多后端路由（OpenAI/Claude/Gemini/LK888/Grsai），能力路由 + SSE + 故障转移 | DDD 四层 |
| `novel-to-comic` | 3000 | 小说转漫画流水线（分段/角色/分镜/生图），SQLite 持久化 + SSE 进度 | 传统 Controller→Service |
| `txt2img` | 8080 | 文生图，SPI 策略多后端（LK888/TokenPlan） | DDD 四层 |
| `comic-workbench` | 8082 | 小说推文漫画工作台（小说库/分镜台/资产/画风/提示词/账号池/预设） | DDD 四层 |

> 注意：`gateway` 与 `comic-workbench` 默认同为 8082，同时启动需改其一端口。

## 构建与运行

Maven 不在 PATH。本机工具链：

```bash
export JAVA_HOME=D:/soft/Java/jdk8
MAVEN=D:/soft/apache-maven-3.6.0/bin/mvn.cmd
$MAVEN -Dmaven.repo.local=D:/apache/noval_repo <goal>
```

- 本地仓库 `D:/apache/noval_repo` **缺少 `okhttp-bom:3.14.9`**，命令行 `mvn` 解析父 POM 会失败。实践中用 **IDEA 运行各模块**，命令行仅做辅助验证。
- 运行单模块：`mvn spring-boot:run -pl <module>`
- 编译单模块：`mvn -pl comic-workbench -am compile`
- 测试单模块单测：`mvn test -pl <module> -Dtest=类名#方法名`
- 访问：gateway Swagger `http://localhost:8082/swagger-ui.html`；comic-workbench `http://localhost:8082`

## 架构约定（重要）

- **DDD 分层**（gateway / txt2img / comic-workbench）：`interfaces → application → domain ← infrastructure`，`domain` 层零外部依赖。详见 `.claude/rules/txt2img-architecture.md`。
- **禁止 Map 作为参数/返回值**，一律用类型化对象（DTO / Command / Result / Value Object）。唯一例外：`/api/models` 顶层信封为前端约定的固定结构。
- **领域常量枚举化**，禁止裸字符串分支：`BackendType`、`ParamType`、`ShotState`、`AssetType`、`PresetType` 等。新增类型时加枚举并提供 `fromString` 之类兼容解析。
- **SPI 策略模式**：新增 AI 后端 = 实现 Provider 接口 + `@Component`，Spring `List<Provider>` 自动发现；**禁止**在 Controller 里用 `if ("xxx".equals(backend))` 选择后端。
- 领域对象不可变（Builder 模式），`List` 字段做防御性拷贝。

## comic-workbench 专项

- 前端为**无构建单页**：`resources/templates/index.html` + `static/js/app.js` + `static/css/styles.css`。改前端后浏览器需 `Ctrl+Shift+R` 强刷。
- SQLite 库文件 `workbench.db`（仓库根目录）。`DatabaseInitializer` 负责建表，并用 `ALTER TABLE` 兼容旧表；启动时自动清理 `%TEMP%\sqlite-*.dll` 残留。
- 主要 REST 端点：
  - `GET/POST/PUT/DELETE /api/novels`，`POST /api/novels/{id}/clean-numbers`，`POST /api/novels/{id}/import-shots`（按行拆分为分镜）
  - `GET /api/shots?novelId&page&size`（`size=0` 查全部），`PUT /api/shots/{id}`，`POST /api/shots/{id}/select-main-image`
  - `POST /api/shots/batch-infer|batch-generate?novelId&scope=page|all&page`
  - `GET/POST /api/presets`，`POST /api/presets/smart?novelId&type=character|scene|prop`
  - `GET/POST /api/comic-settings`，`/api/assets`、`/api/styles`、`/api/prompts`、`/api/accounts`
- 分镜台主流程：小说详情 →「导入剧本」（每行一个 shot）→ 预设设置（左侧滑入面板，角色/场景/道具 Tab）→ 漫画设置（模型/画风/前后缀）→ 批量推理 → 批量生图。
- 分镜实体字段：`originalText`（画面内容）、`tags`（预设标记）、`promptText`（图片描述词）、`mainImageUrl`（主图）、`altImageUrls`（备选图）。

## 环境坑

- **Java 8**：不可用 `String.lines()`、`var`、`List.of()` 等 Java 9+ API；用 `Arrays.stream(s.split(...))` 等替代。
- **sqlite-jdbc 启动失败** `UnsatisfiedLinkError ... sqlite-*.dll 另一个程序正在使用`：每次启动提取随机 UUID 的 DLL，残留被锁。解决：确保旧 Java 进程退出后 `del /Q %TEMP%\sqlite-*.dll`，或依赖启动自动清理逻辑。
- 源码 UTF-8。Windows shell 直接向 curl 传中文 JSON 可能乱码，联调时用 `--data-binary @utf8文件.json`。
- `application-dev.yml` / `*-local.yml` 含本地密钥，已被 `.gitignore` 忽略，勿提交。
