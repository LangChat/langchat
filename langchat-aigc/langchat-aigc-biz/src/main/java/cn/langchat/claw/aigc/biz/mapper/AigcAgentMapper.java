package cn.langchat.claw.aigc.biz.mapper;

import cn.langchat.claw.aigc.biz.entity.AigcAgent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 数据访问接口。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Mapper
public interface AigcAgentMapper extends BaseMapper<AigcAgent> {
}
