import type { WatchSource } from 'vue';

import { nextTick, onScopeDispose, ref, watch } from 'vue';

/** 距底部小于该值视为"贴底",自动跟随滚动 */
const BOTTOM_THRESHOLD = 80;

/**
 * LcChat 滚动控制:内容变化时柔和滚动到底部(平滑动画,非瞬移),
 * 用户向上翻阅时暂停自动跟随,重新贴底后恢复。
 */
export function useChatScroll(
  getContainer: () => HTMLElement | null | undefined,
  source: WatchSource<unknown>,
) {
  const nearBottom = ref(true);
  let scrollRaf = 0;

  function updateNearBottom() {
    const el = getContainer();
    if (!el) {
      return;
    }
    nearBottom.value =
      el.scrollHeight - el.scrollTop - el.clientHeight < BOTTOM_THRESHOLD;
  }

  function scrollToBottom(behavior: ScrollBehavior = 'smooth') {
    const el = getContainer();
    if (!el) {
      return;
    }
    el.scrollTo({ top: el.scrollHeight, behavior });
  }

  // 流式输出时 delta 触发非常频繁,用 rAF 合并滚动请求,保持柔和跟手
  function followSmoothly() {
    if (!nearBottom.value) {
      return;
    }
    cancelAnimationFrame(scrollRaf);
    scrollRaf = requestAnimationFrame(() => {
      nextTick(() => scrollToBottom('smooth'));
    });
  }

  watch(source, () => followSmoothly(), { flush: 'post' });

  onScopeDispose(() => {
    cancelAnimationFrame(scrollRaf);
  });

  return { nearBottom, scrollToBottom, updateNearBottom };
}
