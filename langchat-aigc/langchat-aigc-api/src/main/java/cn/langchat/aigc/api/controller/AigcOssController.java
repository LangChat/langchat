package cn.langchat.aigc.api.controller;

import cn.langchat.aigc.biz.entity.AigcOss;
import cn.langchat.aigc.biz.service.AigcOssService;
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
 * 资源文件管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/aigc/oss")
@RequiredArgsConstructor
@Slf4j
public class AigcOssController {

    private final AigcOssService aigcOssService;

    @GetMapping
    public ApiResponse<List<AigcOss>> list() {
        return ApiResponse.success(aigcOssService.lambdaQuery().orderByDesc(AigcOss::getCreateTime).list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcOss> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcOssService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcOss oss) {
        log.info("新增资源文件，filename={}", oss.getFilename());
        return ApiResponse.success(aigcOssService.save(oss));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcOss oss) {
        oss.setId(id);
        log.info("更新资源文件，id={}", id);
        return ApiResponse.success(aigcOssService.updateById(oss));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除资源文件，id={}", id);
        return ApiResponse.success(aigcOssService.removeById(id));
    }
}
