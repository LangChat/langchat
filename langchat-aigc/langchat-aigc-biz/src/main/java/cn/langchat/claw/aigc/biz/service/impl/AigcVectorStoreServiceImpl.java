package cn.langchat.claw.aigc.biz.service.impl;

import cn.langchat.claw.aigc.biz.entity.AigcVectorStore;
import cn.langchat.claw.aigc.biz.mapper.AigcVectorStoreMapper;
import cn.langchat.claw.aigc.biz.service.AigcVectorStoreService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 向量库 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
public class AigcVectorStoreServiceImpl extends ServiceImpl<AigcVectorStoreMapper, AigcVectorStore> implements AigcVectorStoreService {
}
