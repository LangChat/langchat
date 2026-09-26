import type { GlobalThemeOverrides } from 'naive-ui';

type NaiveCommonTokens = {
  baseColor: string;
  bodyColor: string;
  borderColor: string;
  borderRadius: string;
  cardColor: string;
  dividerColor: string;
  errorColor: string;
  errorColorHover: string;
  errorColorPressed: string;
  modalColor: string;
  popoverColor: string;
  primaryColor: string;
  primaryColorHover: string;
  primaryColorPressed: string;
  successColor: string;
  successColorHover: string;
  successColorPressed: string;
  tableColor: string;
  textColorBase: string;
  warningColor: string;
  warningColorHover: string;
  warningColorPressed: string;
};

export function createNaiveThemeOverrides(
  commonTokens: NaiveCommonTokens,
): GlobalThemeOverrides {
  const border = (color: string) => `1px var(--border-style, solid) ${color}`;
  const dashedBorder = (color: string) => `1px dashed ${color}`;
  const popupShadow = '0 14px 38px hsl(var(--foreground) / 0.12)';

  return {
    common: commonTokens,
    Alert: {
      borderRadius: commonTokens.borderRadius,
    },
    AutoComplete: {
      menuBorderRadius: commonTokens.borderRadius,
      menuBoxShadow: popupShadow,
    },
    Breadcrumb: {
      itemBorderRadius: commonTokens.borderRadius,
    },
    Button: {
      borderFocus: border(commonTokens.primaryColor),
      borderFocusPrimary: border(commonTokens.primaryColorHover),
      borderHover: border(commonTokens.primaryColorHover),
      borderHoverPrimary: border(commonTokens.primaryColorHover),
      borderPressed: border(commonTokens.primaryColorPressed),
      borderPressedPrimary: border(commonTokens.primaryColorPressed),
      borderPrimary: border(commonTokens.primaryColor),
      borderRadiusMedium: commonTokens.borderRadius,
      borderRadiusSmall: commonTokens.borderRadius,
      borderRadiusTiny: commonTokens.borderRadius,
    },
    Card: {
      borderColor: commonTokens.borderColor,
      borderRadius: commonTokens.borderRadius,
      color: commonTokens.cardColor,
    },
    Cascader: {
      menuBorderRadius: commonTokens.borderRadius,
      menuBoxShadow: popupShadow,
    },
    Checkbox: {
      border: border(commonTokens.borderColor),
      borderChecked: border(commonTokens.primaryColor),
      borderDisabled: border(commonTokens.borderColor),
      borderDisabledChecked: border(commonTokens.borderColor),
      borderFocus: border(commonTokens.primaryColor),
      borderRadius: commonTokens.borderRadius,
    },
    ColorPicker: {
      border: border(commonTokens.borderColor),
      borderRadius: commonTokens.borderRadius,
      color: commonTokens.popoverColor,
    },
    DataTable: {
      borderColor: commonTokens.borderColor,
      tdColor: commonTokens.cardColor,
      tdColorHover: commonTokens.bodyColor,
      thColorHover: commonTokens.bodyColor,
    },
    DatePicker: {
      itemBorderRadius: commonTokens.borderRadius,
      panelBorderRadius: commonTokens.borderRadius,
      panelColor: commonTokens.popoverColor,
      scrollItemBorderRadius: commonTokens.borderRadius,
    },
    Descriptions: {
      borderColor: commonTokens.borderColor,
      borderRadius: commonTokens.borderRadius,
    },
    Dialog: {
      borderRadius: commonTokens.borderRadius,
    },
    Drawer: {
      borderRadius: commonTokens.borderRadius,
    },
    Dropdown: {
      borderRadius: commonTokens.borderRadius,
      boxShadow: popupShadow,
      color: commonTokens.popoverColor,
      dividerColor: commonTokens.borderColor,
      optionColorActive: 'hsl(var(--accent-hover))',
      optionColorHover: 'hsl(var(--accent))',
    },
    Input: {
      border: border(commonTokens.borderColor),
      borderDisabled: border(commonTokens.borderColor),
      borderError: border(commonTokens.errorColor),
      borderFocus: border(commonTokens.primaryColor),
      borderFocusError: border(commonTokens.errorColorHover),
      borderFocusWarning: border(commonTokens.warningColorHover),
      borderHover: border(commonTokens.primaryColorHover),
      borderHoverError: border(commonTokens.errorColorHover),
      borderHoverWarning: border(commonTokens.warningColorHover),
      borderRadius: commonTokens.borderRadius,
      borderWarning: border(commonTokens.warningColor),
      boxShadowFocus: 'none',
      boxShadowFocusError: 'none',
      boxShadowFocusWarning: 'none',
    },
    InputNumber: {
      peers: {
        Input: {
          border: border(commonTokens.borderColor),
          borderDisabled: border(commonTokens.borderColor),
          borderError: border(commonTokens.errorColor),
          borderFocus: border(commonTokens.primaryColor),
          borderFocusError: border(commonTokens.errorColorHover),
          borderFocusWarning: border(commonTokens.warningColorHover),
          borderHover: border(commonTokens.primaryColorHover),
          borderHoverError: border(commonTokens.errorColorHover),
          borderHoverWarning: border(commonTokens.warningColorHover),
          borderRadius: commonTokens.borderRadius,
          borderWarning: border(commonTokens.warningColor),
          boxShadowFocus: 'none',
          boxShadowFocusError: 'none',
          boxShadowFocusWarning: 'none',
        },
      },
    },
    Modal: {
      borderRadius: commonTokens.borderRadius,
    },
    Pagination: {
      buttonBorder: border(commonTokens.borderColor),
      buttonBorderHover: border(commonTokens.primaryColorHover),
      buttonBorderPressed: border(commonTokens.primaryColorPressed),
      itemBorder: border('transparent'),
      itemBorderActive: border(commonTokens.primaryColor),
      itemBorderDisabled: border(commonTokens.borderColor),
      itemBorderHover: border(commonTokens.primaryColorHover),
      itemBorderPressed: border(commonTokens.primaryColorPressed),
      itemBorderRadius: commonTokens.borderRadius,
    },
    Popover: {
      borderRadius: commonTokens.borderRadius,
      boxShadow: popupShadow,
      color: commonTokens.popoverColor,
      dividerColor: commonTokens.borderColor,
    },
    Radio: {
      boxShadow: `inset 0 0 0 1px ${commonTokens.borderColor}`,
      boxShadowActive: `inset 0 0 0 1px ${commonTokens.primaryColor}`,
      boxShadowHover: `inset 0 0 0 1px ${commonTokens.primaryColorHover}`,
      buttonBorderColor: commonTokens.borderColor,
      buttonBorderColorActive: commonTokens.primaryColor,
      buttonBorderColorHover: commonTokens.primaryColorHover,
      buttonBorderRadius: commonTokens.borderRadius,
    },
    Select: {
      menuBorderRadius: commonTokens.borderRadius,
      menuBoxShadow: popupShadow,
      peers: {
        InternalSelection: {
          border: border(commonTokens.borderColor),
          borderActive: border(commonTokens.primaryColor),
          borderError: border(commonTokens.errorColor),
          borderFocus: border(commonTokens.primaryColor),
          borderFocusError: border(commonTokens.errorColorHover),
          borderFocusWarning: border(commonTokens.warningColorHover),
          borderHover: border(commonTokens.primaryColorHover),
          borderHoverError: border(commonTokens.errorColorHover),
          borderHoverWarning: border(commonTokens.warningColorHover),
          borderRadius: commonTokens.borderRadius,
          borderWarning: border(commonTokens.warningColor),
          boxShadowActive: 'none',
          boxShadowFocus: 'none',
        },
      },
    },
    Skeleton: {
      borderRadius: commonTokens.borderRadius,
    },
    Tabs: {
      tabBorderColor: commonTokens.borderColor,
      tabBorderRadius: commonTokens.borderRadius,
    },
    Tag: {
      border: border(commonTokens.borderColor),
      borderRadius: commonTokens.borderRadius,
    },
    TimePicker: {
      itemBorderRadius: commonTokens.borderRadius,
      panelBorderRadius: commonTokens.borderRadius,
      panelColor: commonTokens.popoverColor,
    },
    Tooltip: {
      borderRadius: commonTokens.borderRadius,
      boxShadow: popupShadow,
    },
    Transfer: {
      borderColor: commonTokens.borderColor,
      borderRadius: commonTokens.borderRadius,
      dividerColor: commonTokens.borderColor,
      listColor: commonTokens.cardColor,
    },
    Tree: {
      nodeBorderRadius: commonTokens.borderRadius,
    },
    TreeSelect: {
      menuBorderRadius: commonTokens.borderRadius,
      menuBoxShadow: popupShadow,
      peers: {
        InternalSelection: {
          border: border(commonTokens.borderColor),
          borderActive: border(commonTokens.primaryColor),
          borderError: border(commonTokens.errorColor),
          borderFocus: border(commonTokens.primaryColor),
          borderFocusError: border(commonTokens.errorColorHover),
          borderFocusWarning: border(commonTokens.warningColorHover),
          borderHover: border(commonTokens.primaryColorHover),
          borderHoverError: border(commonTokens.errorColorHover),
          borderHoverWarning: border(commonTokens.warningColorHover),
          borderRadius: commonTokens.borderRadius,
          borderWarning: border(commonTokens.warningColor),
          boxShadowActive: 'none',
          boxShadowFocus: 'none',
        },
      },
    },
    Upload: {
      borderRadius: commonTokens.borderRadius,
      draggerBorder: dashedBorder(commonTokens.borderColor),
      draggerBorderHover: dashedBorder(commonTokens.primaryColorHover),
      itemBorderImageCard: border(commonTokens.borderColor),
      itemBorderImageCardError: border(commonTokens.errorColor),
    },
  };
}
