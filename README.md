# Novel AI Tools - AI 创作工具集

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)

**Novel AI Tools** 是一个基于 AI 的小说创作工具集，提供从小说文本到漫画的全流程自动化生成能力。

## 🎯 项目简介

本项目是一个多模块的 Maven 项目，集成了多个 AI 能力，帮助创作者快速将小说文本转化为漫画分镜和图像。

### 核心功能

- **AI 网关（Gateway）** - 统一代理多个 AI 后端，提供标准化的 OpenAI 兼容 API
- **小说转漫画（Novel-to-Comic）** - 自动化流水线，从小说文本生成分镜脚本和漫画图像
- **文生图（TXT2IMG）** - 基于 DDD 架构的图像生成模块，支持多后端切换

## 🏗️ 项目架构

```
noval-tools/
├── gateway/              # AI 网关模块
├── novel-to-comic/       # 小说转漫画流水线
├── txt2img/              # 文生图模块
└── doc/                  # 项目文档
```

## 📦 模块说明

### 1. Gateway - AI 网关

**作用**：统一的 AI 能力代理层，对外提供 OpenAI 兼容协议，内部路由到多个 AI 后端。

**核心特性**：
- OpenAI 兼容协议（`/v1/chat/completions`, `/v1/images/generations`）
- 多后端支持：OpenAI、Claude、Gemini、LK888、Grsai、KL 等
- 能力路由：CHAT / VISION / IMAGE_GEN / IMAGE_EDIT / IMAGE_COMPOSE
- 加权负载均衡和自动故障转移
- 流式响应（SSE）
- API Key 认证和 Swagger 文档

**详细文档**：[gateway/README.md](gateway/README.md)

### 2. Novel-to-Comic - 小说转漫画

**作用**：自动化流水线，将小说文本转化为漫画分镜。

**核心特性**：
- 文本分段和角色提取
- AI 生成分镜脚本
- 自动生成漫画图像
- 流水线状态持久化（SQLite）
- SSE 实时进度推送
- 项目管理（保存/加载/删除）

**流水线步骤**：
1. 文本输入和分段
2. 角色提取
3. 场景分析
4. 分镜脚本生成
5. 图像生成
6. 结果导出

**详细文档**：[novel-to-comic/README.md](novel-to-comic/README.md)

### 3. TXT2IMG - 文生图

**作用**：基于 DDD 四层架构的图像生成模块，支持多后端切换。

**核心特性**：
- DDD 四层架构（interfaces → application → domain ← infrastructure）
- SPI 策略模式，支持多后端自动发现
- 不可变领域对象（Command/Result/Value Objects）
- 支持后端：LK888、TokenPlan
- 类型化模型定义和参数校验

**详细文档**：[txt2img/README.md](txt2img/README.md)

## 🚀 快速开始

### 环境要求

- Java 8+
- Maven 3.6+
- 至少一个 AI 后端的 API Key

### 安装和运行

```bash
# 克隆项目
git clone https://github.com/fangkunx/noval-tools.git
cd noval-tools

# 配置 API Key（编辑 application.yml 或设置环境变量）
export OPENAI_API_KEY=sk-xxx
export CLAUDE_API_KEY=sk-ant-xxx
# ... 其他 API Key

# 编译项目
mvn clean install

# 启动 Gateway（默认端口 8082）
mvn spring-boot:run -pl gateway

# 启动 Novel-to-Comic（端口 8081）
mvn spring-boot:run -pl novel-to-comic

# 启动 TXT2IMG（端口 8080）
mvn spring-boot:run -pl txt2img
```

### 访问地址

- **Gateway**: http://localhost:8082/swagger-ui.html
- **Novel-to-Comic**: http://localhost:8081
- **TXT2IMG**: http://localhost:8080

## 📚 使用示例

### Gateway API 调用

#### 聊天补全

```bash
curl -X POST http://localhost:8082/v1/chat/completions \
  -H "Authorization: Bearer gw-default-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "Hello"}]
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

#### 图片合成（多图网格）

```bash
curl -X POST http://localhost:8082/v1/images/compose \
  -H "Authorization: Bearer gw-default-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "tt-image-2.5",
    "prompt": "combine these images into a 2x2 grid layout",
    "imageUrls": [
      "https://example.com/image1.jpg",
      "https://example.com/image2.jpg",
      "https://example.com/image3.jpg",
      "https://example.com/image4.jpg"
    ],
    "aspectRatio": "3:4",
    "resolution": "2K"
  }'
```

### 批量图片合成

访问 http://localhost:8082/compose-batch.html 使用批量合成界面：

- 一次选择 40 张图片
- 每 4 张一组，自动分成 10 组
- 10 个任务并发处理
- 实时进度显示
- 处理完成后打包下载 ZIP

## 🔧 配置说明

### Gateway 配置

编辑 `gateway/src/main/resources/application.yml`：

```yaml
gateway:
  providers:
    openai:
      api-key: ${OPENAI_API_KEY:}
    claude:
      api-key: ${CLAUDE_API_KEY:}
    google:
      api-key: ${GOOGLE_API_KEY:}
    lk888:
      api-key: ${LK888_API_KEY:}
    grsai:
      api-key: ${GRSAI_API_KEY:}
  qiniu:
    access-key: ${QINIU_ACCESS_KEY:}
    secret-key: ${QINIU_SECRET_KEY:}
    bucket: ${QINIU_BUCKET:}
    domain: ${QINIU_DOMAIN:}
  compose:
    prompt-templates:
      default: "Arrange the input images in a strict rectangular grid layout..."
```

### 环境变量

创建 `.env` 文件或导出环境变量：

```bash
export OPENAI_API_KEY=sk-xxx
export CLAUDE_API_KEY=sk-ant-xxx
export GOOGLE_API_KEY=xxx
export LK888_API_KEY=sk-xxx
export GRSAI_API_KEY=sk-xxx
export QINIU_ACCESS_KEY=xxx
export QINIU_SECRET_KEY=xxx
export QINIU_BUCKET=your-bucket
export QINIU_DOMAIN=https://your-domain.com
```

## 📖 API 文档

完整的 API 文档请访问各模块的 Swagger UI：

- **Gateway**: http://localhost:8082/swagger-ui.html
- **Novel-to-Comic**: http://localhost:8081/swagger-ui.html
- **TXT2IMG**: http://localhost:8080/swagger-ui.html

或查看 [API 文档](doc/API.md)

## 🏛️ 架构设计

### DDD 四层架构

本项目采用领域驱动设计（DDD）的四层架构：

```
interfaces → application → domain ← infrastructure
```

- **interfaces（接口层）**：REST Controller 和 DTO
- **application（应用层）**：编排服务，协调领域对象
- **domain（领域层）**：核心业务逻辑，零依赖
- **infrastructure（基础设施层）**：SPI 实现、持久化、外部服务

### SPI 策略模式

通过 `GatewayProvider` SPI 接口实现多后端支持：

```java
public interface GatewayProvider {
    ProviderType getProviderType();
    List<ModelSpec> getSupportedModels();
    ChatResult chat(ChatCommand command, String modelName);
    ImageGenResult imageGen(ImageGenCommand command, String modelName);
    // ...
}
```

新增后端只需实现该接口并添加 `@Component` 注解。

## 🔐 安全说明

- 所有 API 需要 Bearer Token 认证
- 默认 API Key：`gw-default-key-12345`（生产环境请修改）
- 敏感配置使用环境变量，避免硬编码
- `.gitignore` 已配置忽略敏感文件

## 📊 项目统计

- **开发语言**：Java 8
- **框架**：Spring Boot 2.7
- **数据库**：H2（开发）、SQLite（novel-to-comic）
- **构建工具**：Maven 3.6+
- **代码行数**：约 15,000+ 行

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/amazing-feature`
3. 提交更改：`git commit -m 'Add amazing feature'`
4. 推送分支：`git push origin feature/amazing-feature`
5. 提交 Pull Request

## 📄 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

- [OpenAI](https://openai.com/) - GPT 系列模型
- [Anthropic](https://www.anthropic.com/) - Claude 模型
- [Google](https://deepmind.google/) - Gemini 模型
- [LK888](https://lk888.ai/) - 图像生成 API
- [Spring Boot](https://spring.io/) - 应用框架

## 📧 联系方式

- GitHub: [@fangkunx](https://github.com/fangkunx)
- Email: fangkunx@example.com

---

**如果这个项目对你有帮助，请给一个 ⭐️ Star！**
