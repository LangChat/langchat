package cn.langchat.claw.auth.service.impl;

import cn.langchat.claw.auth.entity.AigcUserRole;
import cn.langchat.claw.auth.mapper.AigcUserRoleMapper;
import cn.langchat.claw.auth.service.AigcUserRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 用户角色关联 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
public class AigcUserRoleServiceImpl extends ServiceImpl<AigcUserRoleMapper, AigcUserRole> implements AigcUserRoleService {
}
