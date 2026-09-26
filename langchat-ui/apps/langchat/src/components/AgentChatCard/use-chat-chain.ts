import type { AgentChatStreamEvent } from '#/api/aigc/chat';
import type {
  AgentChatChainContext,
  AgentChatChainPatch,
  AgentChatEventEntry,
} from './types';

import { ref } from 'vue';

import {
  createAgentChatChainContext,
  parseAgentChatStreamEvent,
} from './event-parser';

function applyPatch(
  entries: AgentChatEventEntry[],
  patch: AgentChatChainPatch,
) {
  if (patch.op === 'append') {
    return [...entries, patch.entry];
  }
  if (patch.op === 'message.delta') {
    return entries.map((item) => {
      if (item.id !== patch.id || item.type !== 'message') {
        return item;
      }
      return {
        ...item,
        content: `${item.content || ''}${patch.delta}`,
      };
    });
  }
  return entries.map((item) => (item.id === patch.id ? patch.entry : item));
}

export function createUserMessageEntry(content: string): AgentChatEventEntry {
  return {
    content,
    id: `user-${Date.now()}`,
    role: 'user',
    time: Date.now(),
    type: 'message',
  };
}

export function createAssistantMessageEntry(content = ''): AgentChatEventEntry {
  return {
    content,
    id: `assistant-${Date.now()}`,
    role: 'assistant',
    time: Date.now(),
    type: 'message',
  };
}

export function useAgentChatChain(initialEntries: AgentChatEventEntry[] = []) {
  const entries = ref<AgentChatEventEntry[]>([...initialEntries]);
  const context = ref<AgentChatChainContext>(createAgentChatChainContext());

  function reset(nextEntries: AgentChatEventEntry[] = []) {
    entries.value = [...nextEntries];
    context.value = createAgentChatChainContext();
  }

  function append(entry: AgentChatEventEntry) {
    const exists = entries.value.some((item) => item.id === entry.id);
    if (exists) {
      entries.value = entries.value.map((item) =>
        item.id === entry.id ? entry : item,
      );
      return;
    }
    entries.value = [...entries.value, entry];
  }

  function appendUserMessage(content: string) {
    const entry = createUserMessageEntry(content);
    append(entry);
    return entry.id;
  }

  function applyPatches(patches: AgentChatChainPatch[]) {
    if (patches.length === 0) {
      return;
    }
    let next = entries.value;
    patches.forEach((patch) => {
      next = applyPatch(next, patch);
    });
    entries.value = next;
  }

  function applyStreamEvent(event: AgentChatStreamEvent) {
    const result = parseAgentChatStreamEvent(event, context.value);
    applyPatches(result.patches);
    return result;
  }

  return {
    append,
    appendUserMessage,
    applyPatches,
    applyStreamEvent,
    context,
    entries,
    reset,
  };
}
