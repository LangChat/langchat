package cn.langchat.aigc.biz.service.impl;

import cn.langchat.aigc.biz.entity.AigcMessageEvent;
import cn.langchat.aigc.biz.mapper.AigcMessageEventMapper;
import cn.langchat.aigc.biz.service.AigcMessageEventService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 消息事件 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/4/6
 */
@Service
public class AigcMessageEventServiceImpl extends ServiceImpl<AigcMessageEventMapper, AigcMessageEvent>
        implements AigcMessageEventService {
}
