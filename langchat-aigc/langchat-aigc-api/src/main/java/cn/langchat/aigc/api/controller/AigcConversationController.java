package cn.langchat.aigc.api.controller;

import cn.langchat.aigc.biz.entity.AigcConversation;
import cn.langchat.aigc.biz.service.AigcConversationService;
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
 * 对话窗口管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/aigc/conversations")
@RequiredArgsConstructor
@Slf4j
public class AigcConversationController {

    private final AigcConversationService aigcConversationService;

    @GetMapping
    public ApiResponse<List<AigcConversation>> list(@RequestParam(value = "agentId", required = false) String agentId) {
        return ApiResponse.success(aigcConversationService.lambdaQuery()
                .eq(agentId != null && !agentId.isBlank(), AigcConversation::getAgentId, agentId)
                .orderByDesc(AigcConversation::getCreateTime)
                .list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcConversation> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcConversationService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcConversation conversation) {
        log.info("新增对话窗口，title={}", conversation.getTitle());
        return ApiResponse.success(aigcConversationService.save(conversation));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcConversation conversation) {
        conversation.setId(id);
        log.info("更新对话窗口，id={}", id);
        return ApiResponse.success(aigcConversationService.updateById(conversation));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除对话窗口，id={}", id);
        return ApiResponse.success(aigcConversationService.removeById(id));
    }
}
