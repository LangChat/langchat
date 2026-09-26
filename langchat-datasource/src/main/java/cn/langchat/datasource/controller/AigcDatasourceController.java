package cn.langchat.datasource.controller;

import cn.langchat.common.core.ApiResponse;
import cn.langchat.datasource.entity.AigcDatasource;
import cn.langchat.datasource.model.DatabaseStructure;
import cn.langchat.datasource.model.SaveStructureRequest;
import cn.langchat.datasource.model.TestConnectionRequest;
import cn.langchat.datasource.service.AigcDatasourceService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * 数据源管理控制器。
 *
 * @author LangChat Team
 * @since 2026/8/27
 */
@RestController
@RequestMapping("/api/v1/datasources")
@RequiredArgsConstructor
@Slf4j
public class AigcDatasourceController {

    private final AigcDatasourceService aigcDatasourceService;

    @GetMapping
    public ApiResponse<List<AigcDatasource>> list() {
        return ApiResponse.success(
                aigcDatasourceService.lambdaQuery().orderByDesc(AigcDatasource::getCreateTime).list()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcDatasource> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcDatasourceService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcDatasource datasource) {
        return ApiResponse.success(aigcDatasourceService.save(datasource));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcDatasource datasource) {
        datasource.setId(id);
        return ApiResponse.success(aigcDatasourceService.updateById(datasource));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        return ApiResponse.success(aigcDatasourceService.removeById(id));
    }

    /**
     * 测试连接。
     */
    @PostMapping("/test")
    public ApiResponse<Boolean> test(@RequestBody TestConnectionRequest request) {
        return ApiResponse.success(aigcDatasourceService.testConnection(request));
    }

    /**
     * 实时内省表结构。
     */
    @GetMapping("/{id}/introspect")
    public ApiResponse<DatabaseStructure> introspect(@PathVariable("id") String id) throws Exception {
        return ApiResponse.success(aigcDatasourceService.introspect(id));
    }

    /**
     * 获取自定义表结构 JSON。
     */
    @GetMapping("/{id}/structure")
    public ApiResponse<Map<String, String>> getStructure(@PathVariable("id") String id) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("structureJson", aigcDatasourceService.getStructure(id));
        return ApiResponse.success(result);
    }

    /**
     * 保存自定义表结构 JSON。
     */
    @PutMapping("/{id}/structure")
    public ApiResponse<Boolean> saveStructure(@PathVariable("id") String id, @RequestBody SaveStructureRequest request) {
        return ApiResponse.success(aigcDatasourceService.saveStructure(id, request.structureJson()));
    }
}
