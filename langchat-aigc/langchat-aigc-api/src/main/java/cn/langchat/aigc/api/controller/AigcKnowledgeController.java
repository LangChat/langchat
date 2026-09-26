package cn.langchat.aigc.api.controller;

import cn.langchat.aigc.biz.entity.AigcKnowledge;
import cn.langchat.aigc.biz.service.AigcKnowledgeService;
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
 * 知识库管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/aigc/knowledges")
@RequiredArgsConstructor
@Slf4j
public class AigcKnowledgeController {

    private final AigcKnowledgeService aigcKnowledgeService;

    @GetMapping
    public ApiResponse<List<AigcKnowledge>> list() {
        return ApiResponse.success(aigcKnowledgeService.lambdaQuery().orderByDesc(AigcKnowledge::getCreateTime).list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcKnowledge> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcKnowledgeService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcKnowledge knowledge) {
        log.info("新增知识库，name={}", knowledge.getName());
        return ApiResponse.success(aigcKnowledgeService.save(knowledge));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcKnowledge knowledge) {
        knowledge.setId(id);
        log.info("更新知识库，id={}", id);
        return ApiResponse.success(aigcKnowledgeService.updateById(knowledge));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除知识库，id={}", id);
        return ApiResponse.success(aigcKnowledgeService.removeById(id));
    }
}
