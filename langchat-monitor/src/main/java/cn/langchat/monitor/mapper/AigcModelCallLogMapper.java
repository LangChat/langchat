package cn.langchat.monitor.mapper;

import cn.langchat.monitor.entity.AigcModelCallLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型调用日志 Mapper。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Mapper
public interface AigcModelCallLogMapper extends BaseMapper<AigcModelCallLog> {
}
