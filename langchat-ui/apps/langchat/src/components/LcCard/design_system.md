# LangChat UI 设计语言规范 (Design System)

本文档旨在梳理并沉淀 LangChat 核心 UI 的设计基因。这种风格融合了**精致的技术感 (Tech-Premium)**与**轻量化的交互体验**，适用于所有核心算力配置及 Agent 交互场景。

## 1. 核心视觉基因 (Visual Identity)

### 1.1 精致的边框艺术 (Border Aesthetics)

- **虚实结合**：默认状态使用 `border-dashed` (虚线边框)，弱化界面的条框感，营造出“配置中”或“待激活”的灵活感；激活或悬停时转为 `border-solid`。
- **动态角落装饰**：通过绝对定位在卡片对角 (Top-Left, Bottom-Right) 放置指示性的小段边框。这种“裁剪框”风格源于精密仪器或测绘软件，极具技术精确感。

### 1.2 动态光影与深度 (Shadow & Depth)

- **弱对比层级**：避免使用阴影和悬浮位移动效，统一通过浅灰背景、细边框和轻微 hover 背景变化建立层级，让界面保持简约、专业和稳定。
- **微克度渐变**：背景色使用 `bg-primary/5` 或 `bg-muted/20` 等低饱和度色彩，通过微小的色阶差异划分区域。

## 2. 交互哲学 (Interaction Philosophy)

### 2.1 状态感官 (State Awareness)

- **实时骨架屏 (Skeleton-First)**：所有数据驱动的组件必须内置骨架屏。这不仅仅是技术占位，更是视觉节奏的一部分。骨架屏应严格对齐组件的物理结构（图标矩形、线条长度）。
- **悬停反馈 (Hover Synergy)**：一次 Hover 触发多重反馈。例如 `LCCard`：边框变色 + 阴影显现 + 对角指示器显现 + 内部图标背景加深。

### 2.2 紧凑与留白 (Compactness & Breath)

- **小字美学 (Small Text)**：在辅助信息（如模型 ID、Base URL）上大量使用 `text-[10px]` 或 `text-[11px]`。配合 `uppercase` 和 `font-medium`，在保持信息密度的同时显现出“专业工具”的精密感。
- **分层容器 (Layered Containers)**：对于核心指标数据，使用 `bg-muted/20` 的背景进行“块状包裹”，在视觉上对信息进行逻辑分类。

## 3. 通用组件化建议 (Componentization)

- **容器化 (Containerization)**：类似于 `LCCard` 的抽象。将复杂的 CSS 效果（渐变、动画）封装在容器内，利用插槽 (Slots) 注入业务内容。
- **标准化模式**：遵循 `src/components/{ComponentName}/index.vue` 的结构，确保组件的自包含性。

## 4. 颜色与调色盘 (Color Palette)

- **Primary**: 用于引导操作、状态指示及装饰性边框。
- **Muted**: 用于非活动背景、辅助线条及次要说明文字（低对比度）。
- **Success/Error**: 严格用于状态点 (Status Dot) 或破坏性操作，保持点缀性质。
