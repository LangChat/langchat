package cn.langchat.aigc.api.controller;

import cn.langchat.aigc.biz.entity.AigcLog;
import cn.langchat.aigc.biz.service.AigcLogService;
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
 * 日志管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/aigc/logs")
@RequiredArgsConstructor
@Slf4j
public class AigcLogController {

    private final AigcLogService aigcLogService;

    @GetMapping
    public ApiResponse<List<AigcLog>> list() {
        return ApiResponse.success(aigcLogService.lambdaQuery().orderByDesc(AigcLog::getCreateTime).list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcLog> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcLogService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcLog aigcLog) {
        log.info("新增日志记录，type={}", aigcLog.getType());
        return ApiResponse.success(aigcLogService.save(aigcLog));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcLog aigcLog) {
        aigcLog.setId(id);
        log.info("更新日志记录，id={}", id);
        return ApiResponse.success(aigcLogService.updateById(aigcLog));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除日志记录，id={}", id);
        return ApiResponse.success(aigcLogService.removeById(id));
    }
}
