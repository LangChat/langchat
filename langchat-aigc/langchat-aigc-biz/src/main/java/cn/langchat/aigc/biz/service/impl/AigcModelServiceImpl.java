package cn.langchat.aigc.biz.service.impl;

import cn.langchat.aigc.biz.entity.AigcModel;
import cn.langchat.aigc.biz.mapper.AigcModelMapper;
import cn.langchat.aigc.biz.service.AigcModelService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 模型 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
public class AigcModelServiceImpl extends ServiceImpl<AigcModelMapper, AigcModel> implements AigcModelService {
}
