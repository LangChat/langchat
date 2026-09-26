package cn.langchat.auth.service.impl;

import cn.langchat.auth.entity.AigcRoleMenu;
import cn.langchat.auth.mapper.AigcRoleMenuMapper;
import cn.langchat.auth.service.AigcRoleMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 角色菜单关联 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
public class AigcRoleMenuServiceImpl extends ServiceImpl<AigcRoleMenuMapper, AigcRoleMenu> implements AigcRoleMenuService {
}
