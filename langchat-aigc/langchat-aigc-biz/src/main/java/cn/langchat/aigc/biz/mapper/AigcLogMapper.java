package cn.langchat.aigc.biz.mapper;

import cn.langchat.aigc.biz.entity.AigcLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 日志 Mapper。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Mapper
public interface AigcLogMapper extends BaseMapper<AigcLog> {
}
