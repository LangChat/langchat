import type {
  AgentChatStreamClientOptions,
  AgentChatStreamRequest,
} from './types';

import { createChatCompletionStreamApi } from '#/api/aigc/chat';

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
