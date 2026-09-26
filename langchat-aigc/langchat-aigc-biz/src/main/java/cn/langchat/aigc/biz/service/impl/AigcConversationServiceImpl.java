package cn.langchat.aigc.biz.service.impl;

import cn.langchat.aigc.biz.entity.AigcConversation;
import cn.langchat.aigc.biz.mapper.AigcConversationMapper;
import cn.langchat.aigc.biz.service.AigcConversationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 对话窗口 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
public class AigcConversationServiceImpl extends ServiceImpl<AigcConversationMapper, AigcConversation> implements AigcConversationService {
}
