package cn.langchat.auth.service.impl;

import cn.langchat.auth.entity.AigcRole;
import cn.langchat.auth.mapper.AigcRoleMapper;
import cn.langchat.auth.service.AigcRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 角色 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
public class AigcRoleServiceImpl extends ServiceImpl<AigcRoleMapper, AigcRole> implements AigcRoleService {
}
