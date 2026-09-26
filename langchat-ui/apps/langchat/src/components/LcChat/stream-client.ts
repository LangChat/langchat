import type {
  AgentChatStreamEvent,
  ChatCompletionRequest,
} from '#/api/aigc/chat';

import { createChatCompletionStreamApi } from '#/api/aigc/chat';

interface AgentChatStreamClientOptions {
  onEvent: (event: AgentChatStreamEvent) => void;
  signal?: AbortSignal;
}

type AgentChatStreamRequest = ChatCompletionRequest;

export async function startAgentChatStream(
  request: AgentChatStreamRequest,
  options: AgentChatStreamClientOptions,
) {
  return createChatCompletionStreamApi(
    request,
    {
      onEvent: (event) => {
        options.onEvent(event);
      },
    },
    options.signal,
  );
}
