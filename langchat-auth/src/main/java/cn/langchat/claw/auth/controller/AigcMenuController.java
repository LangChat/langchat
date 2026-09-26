package cn.langchat.claw.auth.controller;

import cn.langchat.claw.auth.entity.AigcMenu;
import cn.langchat.claw.auth.model.response.AigcMenuTreeNode;
import cn.langchat.claw.auth.service.AigcMenuService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 菜单管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/auth/menus")
@RequiredArgsConstructor
@Slf4j
public class AigcMenuController {

    private final AigcMenuService aigcMenuService;

    @GetMapping
    public ApiResponse<List<AigcMenu>> list() {
        return ApiResponse.success(aigcMenuService.lambdaQuery().orderByAsc(AigcMenu::getOrderNo).list());
    }

    /**
     * 查询树形菜单。
     */
    @GetMapping("/tree")
    public ApiResponse<List<AigcMenuTreeNode>> listTree(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "type", required = false) String type
    ) {
        return ApiResponse.success(aigcMenuService.listTree(keyword, type));
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcMenu> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcMenuService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcMenu menu) {
        log.info("新增菜单，name={}", menu.getName());
        return ApiResponse.success(aigcMenuService.save(menu));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcMenu menu) {
        menu.setId(id);
        log.info("更新菜单，id={}", id);
        return ApiResponse.success(aigcMenuService.updateById(menu));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除菜单，id={}", id);
        return ApiResponse.success(aigcMenuService.removeById(id));
    }
}
