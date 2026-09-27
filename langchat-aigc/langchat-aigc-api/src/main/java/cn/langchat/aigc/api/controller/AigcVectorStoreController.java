package cn.langchat.aigc.api.controller;

import cn.langchat.aigc.biz.entity.AigcVectorStore;
import cn.langchat.aigc.biz.service.AigcVectorStoreService;
import cn.langchat.aigc.biz.support.VectorStoreSupport;
import cn.langchat.common.core.ApiResponse;
import cn.langchat.common.core.CommonErrorCode;
import cn.langchat.common.exception.BizException;
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
        validateVectorStore(vectorStore);
        return ApiResponse.success(aigcVectorStoreService.save(vectorStore));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcVectorStore vectorStore) {
        vectorStore.setId(id);
        log.info("更新向量库，id={}", id);
        validateVectorStore(vectorStore);
        return ApiResponse.success(aigcVectorStoreService.updateById(vectorStore));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除向量库，id={}", id);
        return ApiResponse.success(aigcVectorStoreService.removeById(id));
    }

    /**
     * 校验向量库配置必填项，避免保存后向量化时才因缺少表名/维度等配置失败。
     */
    private void validateVectorStore(AigcVectorStore vectorStore) {
        if (vectorStore.getName() == null || vectorStore.getName().isBlank()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST.code(), "向量库名称不能为空");
        }
        if (vectorStore.getProvider() == null || vectorStore.getProvider().isBlank()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST.code(), "向量库供应商不能为空");
        }
        if (vectorStore.getHost() == null || vectorStore.getHost().isBlank()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST.code(), "向量库主机地址不能为空");
        }
        if (VectorStoreSupport.requiresTableName(vectorStore)
                && (vectorStore.getTableName() == null || vectorStore.getTableName().isBlank())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST.code(), "向量库表名/集合名不能为空");
        }
        if (vectorStore.getDimension() == null || vectorStore.getDimension() <= 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST.code(), "向量库维度必须大于 0");
        }
    }
}
