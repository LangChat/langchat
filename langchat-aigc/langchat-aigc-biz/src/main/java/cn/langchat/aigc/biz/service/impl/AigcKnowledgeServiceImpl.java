package cn.langchat.aigc.biz.service.impl;

import cn.langchat.aigc.biz.entity.AigcKnowledge;
import cn.langchat.aigc.biz.mapper.AigcKnowledgeMapper;
import cn.langchat.aigc.biz.service.AigcKnowledgeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 知识库 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
public class AigcKnowledgeServiceImpl extends ServiceImpl<AigcKnowledgeMapper, AigcKnowledge> implements AigcKnowledgeService {
}
