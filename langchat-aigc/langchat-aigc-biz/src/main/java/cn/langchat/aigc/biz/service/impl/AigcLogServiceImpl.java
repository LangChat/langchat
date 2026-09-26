package cn.langchat.aigc.biz.service.impl;

import cn.langchat.aigc.biz.entity.AigcLog;
import cn.langchat.aigc.biz.mapper.AigcLogMapper;
import cn.langchat.aigc.biz.service.AigcLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 日志 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
public class AigcLogServiceImpl extends ServiceImpl<AigcLogMapper, AigcLog> implements AigcLogService {
}
