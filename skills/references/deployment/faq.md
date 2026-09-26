# 常见问题

## 问题查找渠道

1. 对应模块的 GitHub 仓库 [issue](https://github.com/vbenjs/vue-vben-admin/issues) 搜索
2. 从 [Google](https://www.google.com) 搜索问题
3. 从 [百度](https://www.baidu.com) 搜索问题
4. 在列表找不到问题可以到 [issues](https://github.com/vbenjs/vue-vben-admin/issues) 提问
5. 需要讨论的问题到 [discussions](https://github.com/vbenjs/vue-vben-admin/discussions)

---

## 依赖问题

### git pull 后依赖更新

在 Monorepo 项目下，需要养成每次 `git pull` 后执行 `pnpm install` 的习惯，因为经常会有新的依赖包加入。项目在 `lefthook.yml` 已配置自动执行，但有时会出现问题，建议手动执行。

### 依赖安装失败

- 尝试执行 `pnpm run reinstall`
- 切换手机热点进行依赖安装
- 配置国内镜像，在项目根目录创建 `.npmrc` 文件：

```bash
# .npmrc
registry = https://registry.npmmirror.com/
```

---

## 缓存更新问题

项目配置默认缓存在 `localStorage` 内，版本更新后可能有些配置没改变。

**解决方式：** 每次更新代码时修改 `package.json` 内的 `version` 版本号。因为 localStorage 的 key 是根据版本号来的，更新后版本不同前面的配置会失效，重新登录即可。

---

## 修改配置文件问题

修改 `.env` 等环境文件以及 `vite.config.ts` 文件时，vite 会自动重启服务。自动重启有几率出现问题，请重新运行项目即可解决。

---

## 本地运行报错

由于 vite 在本地没有转换代码，且代码中用到了可选链等比较新的语法，本地开发需要使用版本较高的浏览器（**Chrome 90+**）。

---

## 页面切换后空白

开启路由切换动画，且页面组件存在多个根节点会导致此问题。

**错误示例：**

```vue
<template>
  <!-- 注释也算一个节点 -->
  <h1>text h1</h1>
  <h2>text h2</h2>
</template>
```

**正确示例：**

```vue
<template>
  <div>
    <h1>text h1</h1>
    <h2>text h2</h2>
  </div>
</template>
```

> **提示：**
> - 如果想使用多个根标签，可以禁用路由切换动画
> - template 下面的根注释节点也算一个节点

---

## 本地开发正常，打包后不行

排查是否使用了 `ctx` 变量：

```ts
// ❌ 错误用法 - ctx 未暴露在实例类型内，Vue 官方不推荐使用
import { getCurrentInstance } from 'vue';
getCurrentInstance().ctx.xxxx;
```

---

## 打包文件过大

- 使用精简版进行开发，完整版引用了较多库文件
- 开启 gzip，体积约为原先 1/3
- 可同时开启 brotli 压缩，比 gzip 更好

**注意：**

- `gzip_static` 模块需要 nginx 另外安装，默认未安装
- 开启 `brotli` 也需要 nginx 另外安装模块

---

## 运行错误 - 路径问题

如果出现类似以下错误，请检查项目全路径（包含所有父级路径）**不能出现中文、日文、韩文**：

```ts
[vite] Failed to resolve module import "ant-design-vue/dist/antd.css-vben-adminode_modulesant-design-vuedistantd.css"
```

---

## 控制台路由警告

如果页面能正常打开，以下警告可忽略：

```ts
[Vue Router warn]: No match found for location with path "xxxx"
```

后续 `vue-router` 可能会提供配置项来关闭警告。

---

## 启动报错 - Node.js 版本

出现以下错误时，检查 Node.js 版本是否符合要求：

```bash
TypeError: str.matchAll is not a function
```

---

## nginx 部署 MIME 类型问题

部署到 nginx 后，可能出现以下错误：

```bash
Failed to load module script: Expected a JavaScript module script but the server responded with a MIME type of "application/octet-stream".
```

**解决方式一：** nginx 配置

```bash
http {
    # 如果有此项配置需要注释掉
    # include       mime.types;

    types {
      application/javascript js mjs;
    }
}
```

**解决方式二：** 修改 nginx 的 `mime.types` 文件，将 `application/javascript js;` 改为 `application/javascript js mjs;`

---

## 项目更新

### 无法像 npm 插件一样更新

项目是完整的项目模版，不是插件或安装包，无法像插件一样更新。需要根据业务需求二次开发，自行手动合并升级。

### 更新建议

项目采用 Monorepo 方式管理，核心代码如 `packages/@core`、`packages/effects` 已抽离。只要业务代码没有修改这部分代码，可以直接拉取最新代码合并。

**建议：** 关注仓库动态积极合并，不要长时间积累，否则合并冲突过多。

### Git 更新流程

```bash
# 1. 添加公司 git 源地址
git remote add up gitUrl;

# 2. 提交代码到公司
git push up main

# 3. 同步公司代码
git pull up main

# 4. 同步开源最新代码
git pull origin main
```

---

## 移除百度统计代码

在对应应用的 `index.html` 文件中，删除以下代码：

```html
<script>
  var _hmt = _hmt || [];
  (function () {
    var hm = document.createElement('script');
    hm.src = 'https://hm.baidu.com/hm.js?d20a01273820422b6aa2ee41b6c9414d';
    var s = document.getElementsByTagName('script')[0];
    s.parentNode.insertBefore(hm, s);
  })();
</script>
```
