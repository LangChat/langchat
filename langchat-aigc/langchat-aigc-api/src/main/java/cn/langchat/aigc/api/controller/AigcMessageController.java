package cn.langchat.aigc.api.controller;

import cn.langchat.aigc.biz.entity.AigcMessage;
import cn.langchat.aigc.biz.service.AigcMessageService;
import cn.langchat.common.core.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/aigc/messages")
@RequiredArgsConstructor
@Slf4j
public class AigcMessageController {

    private final AigcMessageService aigcMessageService;

    @GetMapping
    public ApiResponse<List<AigcMessage>> list(@RequestParam(value = "conversationId", required = false) String conversationId) {
        return ApiResponse.success(aigcMessageService.lambdaQuery()
                .eq(conversationId != null && !conversationId.isBlank(), AigcMessage::getConversationId, conversationId)
                .orderByAsc(AigcMessage::getCreateTime)
                .list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcMessage> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcMessageService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcMessage message) {
        log.info("新增消息，conversationId={}, role={}", message.getConversationId(), message.getRole());
        return ApiResponse.success(aigcMessageService.save(message));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcMessage message) {
        message.setId(id);
        log.info("更新消息，id={}", id);
        return ApiResponse.success(aigcMessageService.updateById(message));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除消息，id={}", id);
        return ApiResponse.success(aigcMessageService.removeById(id));
    }
}
