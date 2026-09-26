package cn.langchat.claw.aigc.api.controller;

import cn.langchat.claw.aigc.biz.entity.AigcModel;
import cn.langchat.claw.aigc.biz.service.AigcModelService;
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
 * 模型管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/aigc/models")
@RequiredArgsConstructor
@Slf4j
public class AigcModelController {

    private final AigcModelService aigcModelService;

    @GetMapping
    public ApiResponse<List<AigcModel>> list() {
        return ApiResponse.success(aigcModelService.lambdaQuery().orderByDesc(AigcModel::getCreateTime).list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcModel> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcModelService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcModel model) {
        log.info("新增模型，provider={}, model={}", model.getProvider(), model.getModel());
        return ApiResponse.success(aigcModelService.save(model));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcModel model) {
        model.setId(id);
        log.info("更新模型，id={}", id);
        return ApiResponse.success(aigcModelService.updateById(model));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除模型，id={}", id);
        return ApiResponse.success(aigcModelService.removeById(id));
    }
}
