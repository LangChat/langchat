package cn.langchat.aigc.api.controller;

import cn.langchat.aigc.biz.entity.AigcMessageEvent;
import cn.langchat.aigc.biz.service.AigcMessageEventService;
import cn.langchat.common.core.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息事件控制器。
 *
 * @author LangChat Team
 * @since 2026/4/6
 */
@RestController
@RequestMapping("/api/v1/aigc/message-events")
@RequiredArgsConstructor
@Slf4j
public class AigcMessageEventController {

    private final AigcMessageEventService aigcMessageEventService;

    @GetMapping
    public ApiResponse<List<AigcMessageEvent>> list(
            @RequestParam(value = "chatId", required = false) String chatId,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "messageId", required = false) String messageId
    ) {
        return ApiResponse.success(aigcMessageEventService.lambdaQuery()
                .eq(chatId != null && !chatId.isBlank(), AigcMessageEvent::getChatId, chatId)
                .eq(conversationId != null && !conversationId.isBlank(), AigcMessageEvent::getConversationId, conversationId)
                .eq(messageId != null && !messageId.isBlank(), AigcMessageEvent::getMessageId, messageId)
                .orderByAsc(AigcMessageEvent::getEventIndex)
                .orderByAsc(AigcMessageEvent::getCreateTime)
                .list());
    }
}
