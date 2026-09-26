<script lang="ts" setup>
import type { NotificationItem } from '@vben/layouts';

import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import { AuthenticationLoginExpiredModal } from '@vben/common-ui';
import { useWatermark } from '@vben/hooks';
import {
  ChevronsLeft,
  ChevronsRight,
  CircleHelp,
  Info,
  SvgGithubIcon,
} from '@vben/icons';
import {
  BasicLayout,
  LanguageToggle,
  LockScreen,
  Notification,
  PreferencesButton,
  ThemeToggle,
  UserDropdown,
} from '@vben/layouts';
import { preferences, updatePreferences } from '@vben/preferences';
import { useAccessStore, useUserStore } from '@vben/stores';
import { openWindow } from '@vben/utils';

import { VbenFullScreen, VbenIconButton } from '@vben-core/shadcn-ui';

import { useAuthStore } from '#/store';
import LoginForm from '#/views/_core/authentication/login.vue';

const LANGCHAT_GITHUB_URL = 'https://github.com/langchat-team/langchat';

const notifications = ref<NotificationItem[]>([]);

const router = useRouter();
const userStore = useUserStore();
const authStore = useAuthStore();
const accessStore = useAccessStore();
const { destroyWatermark, updateWatermark } = useWatermark();
const showDot = computed(() =>
  notifications.value.some((item) => !item.isRead),
);

const menus = computed(() => [
  {
    handler: () => {
      router.push({ path: '/about' });
    },
    icon: Info,
    text: '关于 LangChat',
  },
  {
    handler: () => {
      openWindow(LANGCHAT_GITHUB_URL, {
        target: '_blank',
      });
    },
    icon: SvgGithubIcon,
    text: 'GitHub',
  },
  {
    handler: () => {
      openWindow(`${LANGCHAT_GITHUB_URL}/issues`, {
        target: '_blank',
      });
    },
    icon: CircleHelp,
    text: '问题反馈',
  },
]);

const avatar = computed(() => {
  return userStore.userInfo?.avatar ?? preferences.app.defaultAvatar;
});

async function handleLogout() {
  // 退出时携带当前页面地址，登录成功后自动跳回
  await authStore.logout();
}

function toggleSidebar() {
  updatePreferences({
    sidebar: { collapsed: !preferences.sidebar.collapsed },
  });
}

function handleNoticeClear() {
  notifications.value = [];
}

function markRead(id: number | string) {
  const item = notifications.value.find((item) => item.id === id);
  if (item) {
    item.isRead = true;
  }
}

function remove(id: number | string) {
  notifications.value = notifications.value.filter((item) => item.id !== id);
}

function handleMakeAll() {
  notifications.value.forEach((item) => (item.isRead = true));
}

watch(
  () => ({
    enable: preferences.app.watermark,
    content: preferences.app.watermarkContent,
  }),
  async ({ enable, content }) => {
    if (enable) {
      await updateWatermark({
        content:
          content ||
          `${userStore.userInfo?.username} - ${userStore.userInfo?.realName}`,
      });
    } else {
      destroyWatermark();
    }
  },
  {
    immediate: true,
  },
);
</script>

<template>
  <BasicLayout @clear-preferences-and-logout="handleLogout">
    <template #user-dropdown>
      <UserDropdown
        :avatar
        :menus
        :text="userStore.userInfo?.realName"
        :description="userStore.userInfo?.desc"
        tag-text="LangChat"
        @logout="handleLogout"
      />
    </template>
    <template #notification>
      <Notification
        :dot="showDot"
        :notifications="notifications"
        @clear="handleNoticeClear"
        @read="(item) => item.id && markRead(item.id)"
        @remove="(item) => item.id && remove(item.id)"
        @make-all="handleMakeAll"
      />
    </template>
    <template v-if="preferences.sidebar.floatingMode" #sidebar-footer>
      <div class="flex w-full flex-col gap-2">
        <UserDropdown
          :avatar
          :menus
          :text="userStore.userInfo?.realName"
          :description="userStore.userInfo?.desc"
          show-details
          tag-text="LangChat"
          @logout="handleLogout"
        />
        <div class="flex items-center justify-between gap-1 [&_button]:size-8">
          <VbenIconButton
            v-if="preferences.widget.sidebarToggle"
            @click="toggleSidebar"
          >
            <ChevronsRight
              v-if="preferences.sidebar.collapsed"
              class="size-4"
            />
            <ChevronsLeft v-else class="size-4" />
          </VbenIconButton>
          <PreferencesButton
            v-if="preferences.app.enablePreferences"
            @clear-preferences-and-logout="handleLogout"
          />
          <ThemeToggle v-if="preferences.widget.themeToggle" />
          <LanguageToggle v-if="preferences.widget.languageToggle" />
          <VbenFullScreen v-if="preferences.widget.fullscreen" />
        </div>
      </div>
    </template>
    <template #extra>
      <AuthenticationLoginExpiredModal
        v-model:open="accessStore.loginExpired"
        :avatar
      >
        <LoginForm />
      </AuthenticationLoginExpiredModal>
    </template>
    <template #lock-screen>
      <LockScreen :avatar @to-login="handleLogout" />
    </template>
  </BasicLayout>
</template>
