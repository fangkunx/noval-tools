# 七牛云 OSS 配置指南

## 1. 注册七牛云

访问 https://www.qiniu.com/ 注册账号。

## 2. 创建存储空间

1. 登录七牛云控制台
2. 进入「对象存储」→「存储空间」
3. 点击「添加存储空间」
4. 填写：
   - 名称：例如 `image-compose`
   - 存储区域：选择离你最近的区域（如华东、华南）
   - 访问控制：**公有读私有写**（重要！）
5. 点击「确定」创建

## 3. 获取密钥

1. 进入「个人中心」→「密钥管理」
2. 复制：
   - **Access Key** (AK)
   - **Secret Key** (SK)

## 4. 配置 CDN 域名（可选但推荐）

1. 进入「对象存储」→ 选择你的存储空间 → 「域名设置」
2. 七牛云会提供一个测试域名（如 `xxx.clouddn.com`），可以直接使用
3. 如果要绑定自己的域名，需要配置 CNAME

## 5. 配置应用

编辑 `src/main/resources/application.yml`：

```yaml
gateway:
  qiniu:
    access-key: ${QINIU_ACCESS_KEY:你的AK}
    secret-key: ${QINIU_SECRET_KEY:你的SK}
    bucket: ${QINIU_BUCKET:你的存储空间名}
    domain: ${QINIU_DOMAIN:你的CDN域名}  # 如 https://xxx.clouddn.com
```

或者设置环境变量：

```bash
export QINIU_ACCESS_KEY=你的AK
export QINIU_SECRET_KEY=你的SK
export QINIU_BUCKET=image-compose
export QINIU_DOMAIN=https://xxx.clouddn.com
```

## 6. 测试流程

上传图片后的流程：

```
前端 (base64) 
  → 后端接收
  → 上传到七牛云
  → 获取公网 URL
  → 调用 LK888 API (传入 URL)
  → LK888 下载图片
  → 合成图片
  → 返回结果
```

## 7. 注意事项

- **存储空间必须是「公有读」**，否则 LK888 无法下载图片
- 七牛云免费额度：标准存储 10GB/月，下载流量 10GB/月
- 图片会自动保存在 `compose/` 目录下
- 文件名格式：`compose/{timestamp}_{uuid}.jpg`
