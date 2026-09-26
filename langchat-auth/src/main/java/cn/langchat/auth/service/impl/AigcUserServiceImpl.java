package cn.langchat.auth.service.impl;

import cn.langchat.auth.entity.AigcUser;
import cn.langchat.auth.mapper.AigcUserMapper;
import cn.langchat.auth.service.AigcUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 用户 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
public class AigcUserServiceImpl extends ServiceImpl<AigcUserMapper, AigcUser> implements AigcUserService {
}
