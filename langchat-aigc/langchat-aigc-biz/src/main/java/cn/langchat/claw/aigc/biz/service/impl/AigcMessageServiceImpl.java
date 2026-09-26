package cn.langchat.claw.aigc.biz.service.impl;

import cn.langchat.claw.aigc.biz.entity.AigcMessage;
import cn.langchat.claw.aigc.biz.mapper.AigcMessageMapper;
import cn.langchat.claw.aigc.biz.service.AigcMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 消息 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
public class AigcMessageServiceImpl extends ServiceImpl<AigcMessageMapper, AigcMessage> implements AigcMessageService {
}
