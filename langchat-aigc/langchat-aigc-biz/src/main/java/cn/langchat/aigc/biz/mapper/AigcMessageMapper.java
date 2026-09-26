package cn.langchat.aigc.biz.mapper;

import cn.langchat.aigc.biz.entity.AigcMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息 Mapper。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Mapper
public interface AigcMessageMapper extends BaseMapper<AigcMessage> {
}
