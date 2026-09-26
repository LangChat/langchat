<script setup lang="ts">
import { $t } from '@vben/locales';

import SwitchItem from '../switch-item.vue';

defineOptions({
  name: 'PreferenceColorMode',
});

const appColorWeakMode = defineModel<boolean>('appColorWeakMode', {
  default: false,
});

const appColorGrayMode = defineModel<boolean>('appColorGrayMode', {
  default: false,
});

const themeDashedBorder = defineModel<boolean>('themeDashedBorder', {
  default: false,
});
</script>

<template>
  <SwitchItem v-model="appColorWeakMode">
    {{ $t('preferences.theme.weakMode') }}
  </SwitchItem>
  <SwitchItem v-model="appColorGrayMode">
    {{ $t('preferences.theme.grayMode') }}
  </SwitchItem>
  <SwitchItem v-model="themeDashedBorder">
    {{ $t('preferences.theme.dashedBorder') }}
  </SwitchItem>
</template>
