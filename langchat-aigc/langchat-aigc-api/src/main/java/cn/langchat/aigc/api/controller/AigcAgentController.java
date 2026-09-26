package cn.langchat.aigc.api.controller;

import cn.langchat.aigc.biz.entity.AigcAgent;
import cn.langchat.aigc.biz.service.AigcAgentService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/aigc/agents")
@RequiredArgsConstructor
@Slf4j
public class AigcAgentController {

    private final AigcAgentService aigcAgentService;

    /**
     * 查询 Agent 列表。
     */
    @GetMapping
    public ApiResponse<List<AigcAgent>> list() {
        return ApiResponse.success(aigcAgentService.lambdaQuery()
                .orderByDesc(AigcAgent::getCreateTime)
                .list());
    }

    /**
     * 查询 Agent 详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<AigcAgent> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcAgentService.getById(id));
    }

    /**
     * 新增 Agent。
     */
    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcAgent agent) {
        log.info("新增 Agent，agentName={}", agent.getAgentName());
        return ApiResponse.success(aigcAgentService.save(agent));
    }

    /**
     * 更新 Agent。
     */
    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcAgent agent) {
        agent.setId(id);
        log.info("更新 Agent，id={}", id);
        return ApiResponse.success(aigcAgentService.updateById(agent));
    }

    /**
     * 删除 Agent。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除 Agent，id={}", id);
        return ApiResponse.success(aigcAgentService.removeById(id));
    }
}
