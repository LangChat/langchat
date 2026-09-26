package cn.langchat.monitor.service.impl;

import cn.langchat.monitor.entity.AigcModelCallLog;
import cn.langchat.monitor.mapper.AigcModelCallLogMapper;
import cn.langchat.monitor.service.AigcModelCallLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 模型调用日志 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Service
public class AigcModelCallLogServiceImpl
        extends ServiceImpl<AigcModelCallLogMapper, AigcModelCallLog>
        implements AigcModelCallLogService {
}
