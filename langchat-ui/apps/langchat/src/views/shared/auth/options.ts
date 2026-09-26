// 使用 type 而非 interface，以便直接赋值给 naive-ui 的 SelectMixedOption[]
export type AuthLabelOption = {
  label: string;
  value: number | string;
};

export const USER_STATUS_OPTIONS: AuthLabelOption[] = [
  { label: '启用', value: 1 },
  { label: '锁定', value: 0 },
];

export const USER_SEX_OPTIONS: AuthLabelOption[] = [
  { label: '未知', value: 'UNKNOWN' },
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' },
];

export const MENU_TYPE_OPTIONS: AuthLabelOption[] = [
  { label: '目录', value: 'CATALOG' },
  { label: '菜单', value: 'MENU' },
  { label: '按钮', value: 'BUTTON' },
];

/**
 * 根据值解析标签。
 */
export function findAuthOptionLabel(
  options: AuthLabelOption[],
  value?: null | number | string,
) {
  if (value === null || value === undefined || value === '') {
    return '未配置';
  }
  return (
    options.find((option) => option.value === value)?.label ?? String(value)
  );
}
