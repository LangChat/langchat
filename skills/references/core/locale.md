# 国际化

项目使用 [Vue i18n](https://kazupon.github.io/vue-i18n/) 进行国际化处理。

## 基础用法

```ts
// 路由配置中使用
import { $t } from '#/locales';

const routes = [
  {
    meta: {
      title: $t('page.home.title'),
    },
    name: 'Home',
    path: '/home',
    component: () => import('#/views/home/index.vue'),
  },
];
```

```vue
<!-- 组件中使用 -->
<script setup lang="ts">
import { $t } from '#/locales';
</script>

<template>
  <div>{{ $t('common.confirm') }}</div>
</template>
```

## 语言配置

在 `preferences.ts` 中设置默认语言：

```ts
import { defineOverridesPreferences } from '@vben/preferences';

export const overridesPreferences = defineOverridesPreferences({
  app: {
    locale: 'zh-CN',  // 'en-US' | 'zh-CN' | ...
  },
});
```

## 支持的语言

```ts
type SupportedLanguagesType =
  | 'en-US'
  | 'zh-CN'
  | 'zh-TW'
  | 'ko-KR'
  | 'ru-RU'
  | 'ja-JP';
```

## 语言包位置

```
packages/locales/
├── langs/           # 语言包文件
│   ├── en-US.json
│   ├── zh-CN.json
│   └── ...
└── src/             # 国际化相关代码
```

## 新增语言包

1. 在 `packages/locales/langs/` 目录下新建语言文件，如 `ja-JP.json`
2. 在 `packages/types/src.ts` 中添加类型定义
3. 在 `preferences.ts` 中配置 `locale: 'ja-JP'`

## 语言包结构示例

```json
{
  "common": {
    "confirm": "确认",
    "cancel": "取消",
    "save": "保存",
    "delete": "删除",
    "search": "搜索",
    "reset": "重置"
  },
  "page": {
    "home": {
      "title": "首页",
      "welcome": "欢迎"
    }
  },
  "ui": {
    "formRules": {
      "required": "请输入{0}",
      "selectRequired": "请选择{0}"
    },
    "placeholder": {
      "input": "请输入",
      "select": "请选择"
    }
  }
}
```

## 远程加载语言包

```ts
// 支持从远程服务器加载语言包
import { setI18nLanguage } from '@vben/locales';

async function loadLocaleMessages(locale: string) {
  const messages = await fetch(`/locales/${locale}.json`).then(res => res.json());
  setI18nLanguage(locale, messages);
}
```

## 切换语言

框架内置了语言切换组件，可通过偏好设置开启：

```ts
export const overridesPreferences = defineOverridesPreferences({
  widget: {
    languageToggle: true,  // 显示语言切换按钮
  },
});
```

## 在代码中切换

```ts
import { useLocale } from '@vben/locales';

const { changeLocale } = useLocale();

// 切换到英文
changeLocale('en-US');
```
