import { $t } from '@vben/locales';

// 使用 type 而非 interface，以便直接赋值给 naive-ui 的 SelectMixedOption[]
export type AuthLabelOption = {
  label: string;
  value: number | string;
};

export function userStatusOptions(): AuthLabelOption[] {
  return [
    { label: $t('common.status.enabled'), value: 1 },
    { label: $t('users.card.locked'), value: 0 },
  ];
}

export function userSexOptions(): AuthLabelOption[] {
  return [
    { label: $t('common.status.unknown'), value: 'UNKNOWN' },
    { label: $t('users.gender.male'), value: 'MALE' },
    { label: $t('users.gender.female'), value: 'FEMALE' },
  ];
}

export function menuTypeOptions(): AuthLabelOption[] {
  return [
    { label: $t('menus.typeOptions.catalog'), value: 'CATALOG' },
    { label: $t('menus.typeOptions.menu'), value: 'MENU' },
    { label: $t('menus.typeOptions.button'), value: 'BUTTON' },
  ];
}

/**
 * 根据值解析标签。
 */
export function findAuthOptionLabel(
  options: AuthLabelOption[],
  value?: null | number | string,
) {
  if (value === null || value === undefined || value === '') {
    return $t('common.status.notConfigured');
  }
  return (
    options.find((option) => option.value === value)?.label ?? String(value)
  );
}
