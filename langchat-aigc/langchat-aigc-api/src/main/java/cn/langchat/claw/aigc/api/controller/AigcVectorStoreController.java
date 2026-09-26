package cn.langchat.claw.aigc.api.controller;

import cn.langchat.claw.aigc.biz.entity.AigcVectorStore;
import cn.langchat.claw.aigc.biz.service.AigcVectorStoreService;
import cn.langchat.claw.common.core.ApiResponse;
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
 * 向量库管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/aigc/vector-stores")
@RequiredArgsConstructor
@Slf4j
public class AigcVectorStoreController {

    private final AigcVectorStoreService aigcVectorStoreService;

    @GetMapping
    public ApiResponse<List<AigcVectorStore>> list() {
        return ApiResponse.success(aigcVectorStoreService.lambdaQuery().orderByDesc(AigcVectorStore::getCreateTime).list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcVectorStore> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcVectorStoreService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcVectorStore vectorStore) {
        log.info("新增向量库，name={}, provider={}", vectorStore.getName(), vectorStore.getProvider());
        return ApiResponse.success(aigcVectorStoreService.save(vectorStore));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcVectorStore vectorStore) {
        vectorStore.setId(id);
        log.info("更新向量库，id={}", id);
        return ApiResponse.success(aigcVectorStoreService.updateById(vectorStore));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除向量库，id={}", id);
        return ApiResponse.success(aigcVectorStoreService.removeById(id));
    }
}
