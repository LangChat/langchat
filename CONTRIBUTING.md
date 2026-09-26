# Contributing to LangChat

感谢你为 LangChat 提交问题、建议和代码。为了让协作过程清晰可追踪，请遵循以下约定。

## 开始之前

- 功能建议和较大改动请先创建 Issue，说明使用场景、预期行为和备选方案。
- 安全漏洞不要创建公开 Issue，请按照 [SECURITY.md](SECURITY.md) 私下报告。
- 当前开发基线为 `next` 分支，Pull Request 应以 `next` 为目标分支。

## 本地开发

后端环境：JDK 17+、Maven 3.9+、MySQL 8+。

前端环境：Node.js 22.22+、pnpm 10.32+。

```bash
git clone -b next https://github.com/LangChat/langchat.git
cd langchat
cp langchat-server/src/main/resources/application-local.example.yml \
  langchat-server/src/main/resources/application-local.yml

cd langchat-ui
corepack enable
pnpm install
cd ..
```

不要提交 `application-local.yml`、密钥、令牌、数据库密码、运行日志或 IDE 工作区文件。

## 分支与提交

从最新的 `next` 创建分支：

```bash
git switch next
git pull --ff-only
git switch -c feat/short-description
```

建议使用清晰的提交前缀：

- `feat:` 新功能
- `fix:` 缺陷修复
- `docs:` 文档更新
- `refactor:` 不改变外部行为的重构
- `test:` 测试变更
- `chore:` 工程与维护变更

一次 Pull Request 聚焦一个问题。避免同时提交无关格式化、依赖升级或重构。

## 验证

提交前至少运行与改动相关的检查。

后端：

```bash
mvn -pl langchat-server -am test
```

前端：

```bash
pnpm --dir langchat-ui --filter @vben/web-naive typecheck
pnpm --dir langchat-ui --filter @vben/web-naive build
```

如果某项检查无法运行，请在 Pull Request 中说明原因和剩余风险。

## Pull Request 要求

- 描述问题、解决方案和用户可见影响。
- 关联对应 Issue。
- 列出验证命令和结果。
- UI 改动提供截图或录屏。
- 数据库或配置变更说明迁移与兼容策略。
- 不包含敏感信息、生成产物或与改动无关的文件。

提交贡献即表示你有权提交相关代码，并同意该贡献按照适用的项目许可证进行分发。

## 行为准则

所有参与者都需要遵守 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。
