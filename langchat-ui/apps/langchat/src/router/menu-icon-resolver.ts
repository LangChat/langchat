import type { Component } from 'vue';

import type { RouteRecordStringComponent } from '@vben/types';

import * as LucideIcons from '@vben/icons';
import { CircleHelp } from '@vben/icons';
import { cloneDeep, mapTree } from '@vben/utils';

type RouteWithIcon = RouteRecordStringComponent & {
  icon?: Component | string;
  meta?: {
    icon?: Component | string;
  };
};

const EXPLICIT_MENU_ICON_MAP: Record<string, Component> = {
  'lucide:bot-message-square': LucideIcons.BotMessageSquare,
  'lucide:brain-circuit': LucideIcons.BrainCircuit,
  'lucide:database-zap': LucideIcons.DatabaseZap,
  'lucide:file-stack': LucideIcons.FileStack,
  'lucide:library-big': LucideIcons.LibraryBig,
  'lucide:menu-square': LucideIcons.SquareMenu,
  'lucide:plug-zap': LucideIcons.PlugZap,
  'lucide:shield': LucideIcons.Shield,
  'lucide:shield-check': LucideIcons.ShieldCheck,
  'lucide:users': LucideIcons.Users,
  'lucide:wrench': LucideIcons.Wrench,
};

const LUCIDE_ICON_NAMESPACE = 'lucide:';
const lucideIconsMap = LucideIcons as Record<string, unknown>;

function isComponent(value: unknown): value is Component {
  return !!value && (typeof value === 'function' || typeof value === 'object');
}

function toPascalCaseIconName(input: string) {
  const normalized = input
    .replace(/^lucide:/i, '')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[-_\s]+/g, ' ')
    .trim();

  if (!normalized) {
    return '';
  }

  return normalized
    .split(' ')
    .filter(Boolean)
    .map((segment) => `${segment[0]!.toUpperCase()}${segment.slice(1)}`)
    .join('');
}

function resolveLucideIcon(icon: string): Component | undefined {
  const normalized = icon.trim();
  if (!normalized) {
    return undefined;
  }

  const explicit = EXPLICIT_MENU_ICON_MAP[normalized.toLowerCase()];
  if (explicit) {
    return explicit;
  }

  if (!normalized.toLowerCase().startsWith(LUCIDE_ICON_NAMESPACE)) {
    return undefined;
  }

  const iconName = toPascalCaseIconName(normalized);
  const resolved = lucideIconsMap[iconName];
  return isComponent(resolved) ? resolved : undefined;
}

function resolveMenuIcon(icon: unknown): Component | string | undefined {
  if (isComponent(icon) || typeof icon !== 'string') {
    return icon as Component | string | undefined;
  }

  const trimmed = icon.trim();
  if (!trimmed) {
    return undefined;
  }

  const resolved = resolveLucideIcon(trimmed);
  return (
    resolved ??
    (trimmed.toLowerCase().startsWith(LUCIDE_ICON_NAMESPACE)
      ? CircleHelp
      : trimmed)
  );
}

export function normalizeMenuIcons(
  routes: RouteRecordStringComponent[],
): RouteRecordStringComponent[] {
  return mapTree(cloneDeep(routes), (node) => {
    const route = node as RouteWithIcon;

    route.icon = resolveMenuIcon(route.icon);
    if (route.meta) {
      route.meta.icon = resolveMenuIcon(route.meta.icon);
    }

    return route as RouteRecordStringComponent;
  });
}
