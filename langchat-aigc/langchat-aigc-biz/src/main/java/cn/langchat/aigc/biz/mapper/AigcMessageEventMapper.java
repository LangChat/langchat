package cn.langchat.aigc.biz.mapper;

import cn.langchat.aigc.biz.entity.AigcMessageEvent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息事件 Mapper。
 *
 * @author LangChat Team
 * @since 2026/4/6
 */
@Mapper
public interface AigcMessageEventMapper extends BaseMapper<AigcMessageEvent> {
}
