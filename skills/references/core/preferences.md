# 偏好设置完整配置

## App 配置

```ts
interface AppPreferences {
  accessMode: 'frontend' | 'backend' | 'mixed';  // 权限模式
  authPageLayout: AuthPageLayoutType;            // 登录页布局
  checkUpdatesInterval: number;                  // 检查更新间隔
  colorGrayMode: boolean;                        // 灰色模式
  colorWeakMode: boolean;                        // 色弱模式
  compact: boolean;                              // 紧凑模式
  contentCompact: 'wide' | 'full';              // 内容紧凑模式
  contentCompactWidth: number;                   // 内容宽度
  contentPadding: number;                        // 内容内边距
  defaultAvatar: string;                         // 默认头像
  defaultHomePath: string;                       // 默认首页路径
  dynamicTitle: boolean;                         // 动态标题
  enableCheckUpdates: boolean;                   // 检查更新
  enablePreferences: boolean;                    // 显示偏好设置
  enableCopyPreferences: boolean;                // 复制偏好设置按钮
  enableRefreshToken: boolean;                   // 刷新Token
  isMobile: boolean;                             // 移动端模式
  layout: LayoutType;                            // 布局方式
  locale: 'zh-CN' | 'en-US';                     // 语言
  loginExpiredMode: 'page' | 'modal';           // 登录过期模式
  name: string;                                  // 应用名
  preferencesButtonPosition: string;             // 偏好设置按钮位置
  watermark: boolean;                            // 水印
  zIndex: number;                                // z-index
}
```

## Theme 配置

```ts
interface ThemePreferences {
  builtinType: BuiltinThemeType;  // 内置主题
  colorDestructive: string;       // 错误色
  colorPrimary: string;           // 主题色
  colorSuccess: string;           // 成功色
  colorWarning: string;           // 警告色
  mode: 'light' | 'dark';         // 主题模式
  radius: string;                 // 圆角
  semiDarkHeader: boolean;        // 半深色顶栏
  semiDarkSidebar: boolean;       // 半深色侧边栏
}
```

内置主题列表：
- `default`, `violet`, `pink`, `rose`, `sky-blue`, `deep-blue`
- `green`, `deep-green`, `orange`, `yellow`
- `zinc`, `neutral`, `slate`, `gray`, `custom`

## Sidebar 配置

```ts
interface SidebarPreferences {
  autoActivateChild: boolean;      // 点击目录自动激活子菜单
  collapsed: boolean;              // 折叠状态
  collapsedButton: boolean;        // 折叠按钮可见
  collapsedShowTitle: boolean;     // 折叠时显示标题
  collapseWidth: number;           // 折叠宽度
  enable: boolean;                 // 启用侧边栏
  expandOnHover: boolean;          // 悬停展开
  extraCollapse: boolean;          // 扩展区域折叠
  extraCollapsedWidth: number;     // 扩展区域折叠宽度
  fixedButton: boolean;            // 固定按钮
  hidden: boolean;                 // 隐藏侧边栏
  mixedWidth: number;              // 混合布局宽度
  width: number;                   // 侧边栏宽度
}
```

## Tabbar 配置

```ts
interface TabbarPreferences {
  draggable: boolean;              // 拖拽
  enable: boolean;                 // 启用标签页
  height: number;                  // 高度
  keepAlive: boolean;              // 缓存
  maxCount: number;                // 最大数量
  middleClickToClose: boolean;     // 中键关闭
  persist: boolean;                // 持久化
  showIcon: boolean;               // 显示图标
  showMaximize: boolean;           // 最大化按钮
  showMore: boolean;               // 更多按钮
  styleType: TabsStyleType;        // 样式类型
  wheelable: boolean;              // 滚轮响应
}
```

## Header 配置

```ts
interface HeaderPreferences {
  enable: boolean;                 // 启用顶栏
  height: number;                  // 高度
  hidden: boolean;                 // 隐藏
  menuAlign: 'start' | 'center' | 'end';  // 菜单对齐
  mode: 'fixed' | 'static';        // 显示模式
}
```

## Breadcrumb 配置

```ts
interface BreadcrumbPreferences {
  enable: boolean;                 // 启用面包屑
  hideOnlyOne: boolean;            // 仅一个时隐藏
  showHome: boolean;               // 显示首页
  showIcon: boolean;               // 显示图标
  styleType: 'normal' | 'background';  // 样式
}
```

## Navigation 配置

```ts
interface NavigationPreferences {
  accordion: boolean;              // 手风琴模式
  split: boolean;                  // 分割（mixed-nav布局）
  styleType: 'rounded' | 'plain';  // 样式
}
```

## Widget 配置

```ts
interface WidgetPreferences {
  fullscreen: boolean;             // 全屏按钮
  globalSearch: boolean;           // 全局搜索
  languageToggle: boolean;         // 语言切换
  lockScreen: boolean;             // 锁屏
  notification: boolean;           // 通知
  refresh: boolean;                // 刷新按钮
  sidebarToggle: boolean;          // 侧边栏切换
  themeToggle: boolean;            // 主题切换
}
```

## Copyright 配置

```ts
interface CopyrightPreferences {
  companyName: string;             // 公司名
  companySiteLink: string;         // 公司链接
  date: string;                    // 日期
  enable: boolean;                 // 启用版权
  icp: string;                     // 备案号
  icpLink: string;                 // 备案链接
  settingShow: boolean;            // 设置面板显示
}
```

## Transition 配置

```ts
interface TransitionPreferences {
  enable: boolean;                 // 启用动画
  loading: boolean;                // 加载动画
  name: PageTransitionType;        // 动画名称
  progress: boolean;               // 进度条
}
```

## 配置示例

```ts
import { defineOverridesPreferences } from '@vben/preferences';

export const overridesPreferences = defineOverridesPreferences({
  app: {
    layout: 'sidebar-nav',
    locale: 'zh-CN',
    dynamicTitle: true,
    accessMode: 'frontend',
    defaultHomePath: '/dashboard',
  },
  theme: {
    mode: 'light',
    builtinType: 'default',
    colorPrimary: 'hsl(212 100% 45%)',
  },
  sidebar: {
    collapsed: false,
    width: 224,
  },
  tabbar: {
    enable: true,
    keepAlive: true,
    styleType: 'chrome',
  },
  header: {
    enable: true,
    height: 50,
  },
  widget: {
    fullscreen: true,
    refresh: true,
    themeToggle: true,
  },
});
```

**注意**：修改配置后需清空浏览器缓存才能生效。
