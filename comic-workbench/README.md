# comic-workbench 小说推文漫画工作台

## 概述

`comic-workbench` 是一个基于 Spring Boot 的小说推文漫画工作台模块，实现了原型设计中的六大核心功能模块：

1. **账号池** - API 服务管理（支持多后端、多协议）
2. **提示词** - 可复用的推理模板管理
3. **画风库** - 画风提示词模板管理
4. **小说库** - 小说/剧本内容管理
5. **分镜台** - 分镜推理与图片生成工作区
6. **资产库** - 角色/场景/道具资产管理

## 架构设计

采用 DDD 四层架构，与 `txt2img` 模块保持一致：

```
com.noval.workbench
├── WorkbenchApplication.java           [Spring Boot 入口]
│
├── interfaces/rest                     [接口层]
│   ├── PageController.java             [页面渲染]
│   ├── AccountController.java          [账号池 REST API]
│   ├── PromptController.java           [提示词 REST API]
│   ├── ArtStyleController.java         [画风库 REST API]
│   ├── NovelController.java            [小说库 REST API]
│   ├── ShotController.java             [分镜台 REST API]
│   ├── AssetController.java            [资产库 REST API]
│   └── dto/                            [请求/响应 DTO]
│
├── application                         [应用层]
│   ├── AccountService.java
│   ├── PromptService.java
│   ├── ArtStyleService.java
│   ├── NovelService.java
│   ├── ShotService.java
│   └── AssetService.java
│
├── domain                              [领域层 —— 零外部依赖]
│   ├── model/                          [领域模型]
│   │   ├── Account.java                [账号实体]
│   │   ├── Prompt.java                 [提示词实体]
│   │   ├── ArtStyle.java               [画风实体]
│   │   ├── Novel.java                  [小说实体]
│   │   ├── Shot.java                   [分镜实体]
│   │   ├── Asset.java                  [资产实体]
│   │   └── [枚举: AccountCapability, AssetType, ContentStyle, ShotState, TechProtocol]
│   └── repository/                     [仓储接口]
│
└── infrastructure                      [基础设施层]
    ├── config/                         [配置类]
    │   ├── DataSourceConfig.java       [SQLite 数据源]
    │   └── JdbcConfig.java             [JDBC 模板]
    ├── persistence/
    │   └── DatabaseInitializer.java    [数据库初始化]
    └── repository/                     [仓储实现]
        └── Jdbc*Repository.java        [6 个 JDBC 仓储实现]
```

### 依赖方向
```
interfaces → application → domain ← infrastructure
```

## 技术栈

- **后端**: Spring Boot 2.7.18, Java 8+
- **数据库**: SQLite (零配置，文件级存储)
- **前端**: 原生 JS + Thymeleaf 模板
- **构建**: Maven

## 快速启动

### 1. 编译

```bash
cd D:\soft\漫画\noval_fj\java
mvn clean compile -pl comic-workbench -am
```

### 2. 运行

```bash
cd comic-workbench
mvn spring-boot:run
```

或者打包后运行：

```bash
mvn clean package -pl comic-workbench -am
java -jar target/comic-workbench-1.0.0.jar
```

### 3. 访问

打开浏览器访问: **http://localhost:8082**

## API 接口

### 账号池 `/api/accounts`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/accounts` | 列出所有账号 |
| GET | `/api/accounts/{id}` | 获取单个账号 |
| POST | `/api/accounts` | 创建账号 |
| PUT | `/api/accounts/{id}` | 更新账号 |
| DELETE | `/api/accounts/{id}` | 删除账号 |
| POST | `/api/accounts/{id}/toggle` | 切换启用状态 |

### 提示词 `/api/prompts`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/prompts` | 列出所有提示词 |
| GET | `/api/prompts?category={category}` | 按分类筛选 |
| GET | `/api/prompts/{id}` | 获取单个 |
| POST | `/api/prompts` | 创建 |
| PUT | `/api/prompts/{id}` | 更新 |
| DELETE | `/api/prompts/{id}` | 删除 |

### 画风库 `/api/styles`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/styles` | 列出所有画风 |
| GET | `/api/styles/{id}` | 获取单个 |
| POST | `/api/styles` | 创建 |
| PUT | `/api/styles/{id}` | 更新 |
| DELETE | `/api/styles/{id}` | 删除 |

### 小说库 `/api/novels`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/novels` | 列出所有小说 |
| GET | `/api/novels/{id}` | 获取单个 |
| POST | `/api/novels` | 创建 |
| PUT | `/api/novels/{id}` | 更新 |
| DELETE | `/api/novels/{id}` | 删除 |

### 分镜台 `/api/shots`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/shots?novelId={id}` | 列出某小说的所有分镜 |
| GET | `/api/shots/{id}` | 获取单个 |
| POST | `/api/shots` | 创建 |
| PUT | `/api/shots/{id}` | 更新 |
| DELETE | `/api/shots/{id}` | 删除 |
| POST | `/api/shots/batch-infer?novelId={id}` | 批量推理 |
| POST | `/api/shots/batch-generate?novelId={id}` | 批量生图 |

### 资产库 `/api/assets`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/assets` | 列出所有资产 |
| GET | `/api/assets?type={CHARACTER\|SCENE\|PROP}` | 按类型筛选 |
| GET | `/api/assets/{id}` | 获取单个 |
| POST | `/api/assets` | 创建 |
| PUT | `/api/assets/{id}` | 更新 |
| DELETE | `/api/assets/{id}` | 删除 |

## 数据库

使用 SQLite 文件数据库 `workbench.db`，首次启动时自动创建表和初始化 schema。

表结构：
- `account` - 账号池
- `prompt` - 提示词
- `art_style` - 画风
- `novel` - 小说
- `shot` - 分镜
- `asset` - 资产

## 前端

前端使用原型设计中的暗色主题 UI，已集成到 Spring Boot 静态资源中：

- `src/main/resources/templates/index.html` - 主页面模板
- `src/main/resources/static/css/styles.css` - 样式
- `src/main/resources/static/js/app.js` - 前端逻辑（调用 REST API）

## 扩展指南

### 添加新字段

1. 在 `domain/model/` 中修改对应的实体类
2. 修改 `infrastructure/persistence/DatabaseInitializer.java` 中的建表 SQL
3. 修改对应的 `Jdbc*Repository.java` 中的 RowMapper 和 SQL
4. 修改 `interfaces/rest/dto/` 中的 DTO 类

### 接入真实 AI 后端

参考 `txt2img` 模块的 SPI 模式：

1. 在 `domain/spi/` 中定义接口
2. 在 `infrastructure/provider/` 中实现具体后端
3. 在 `application/` 中编排调用

## 与其他模块的关系

- **txt2img** - 独立的文本生图工具，提供图片生成能力
- **novel-to-comic** - 小说转漫画的完整流水线
- **comic-workbench** - 工作台 UI 和数据管理（本模块）
- **gateway** - API 网关，可路由到各模块

## 许可证

与项目整体保持一致。
