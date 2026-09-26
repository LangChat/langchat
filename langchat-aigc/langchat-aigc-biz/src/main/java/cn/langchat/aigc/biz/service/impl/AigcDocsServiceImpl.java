package cn.langchat.aigc.biz.service.impl;

import cn.langchat.aigc.biz.entity.AigcDocs;
import cn.langchat.aigc.biz.mapper.AigcDocsMapper;
import cn.langchat.aigc.biz.service.AigcDocsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 文档 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
public class AigcDocsServiceImpl extends ServiceImpl<AigcDocsMapper, AigcDocs> implements AigcDocsService {
}
