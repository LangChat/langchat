import {defineOverridesPreferences} from '@vben/preferences';

/**
 * @description 项目配置文件
 * 只需要覆盖项目中的一部分配置，不需要的配置不用覆盖，会自动使用默认配置
 * !!! 更改配置后请清空缓存，否则可能不生效
 */
export const overridesPreferences = defineOverridesPreferences({
  // overrides
  app: {
    name: 'LangChat',
    accessMode: 'mixed',
    dynamicTitle: true,
    enableCheckUpdates: true,
    defaultHomePath: '/explore',
    watermark: true,
  },
  logo: {
    enable: true,
    source: '/logo.png',
  },
  footer: {
    enable: false,
  },
  theme: {
    mode: 'light',
    builtinType: 'default',
    colorPrimary: 'hsl(212 100% 45%)',
    dashedBorder: true,
    radius: '0'
  },
  transition: {
    name: 'fade',
  },
  widget: {
    globalSearch: false,
    lockScreen: false,
    notification: false,
    refresh: false,
    timezone: false,
  },
  copyright: {
    companyName: 'LangChat Team',
    companySiteLink: 'https://langchat.cn',
    date: '2026',
    enable: true,
  },
  navigation: {
    flatGroupMode: true,
  },
  sidebar: {
    floatingMode: true,
  },
});
