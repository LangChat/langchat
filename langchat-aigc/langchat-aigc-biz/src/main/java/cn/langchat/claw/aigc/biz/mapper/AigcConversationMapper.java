package cn.langchat.claw.aigc.biz.mapper;

import cn.langchat.claw.aigc.biz.entity.AigcConversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话窗口 Mapper。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Mapper
public interface AigcConversationMapper extends BaseMapper<AigcConversation> {
}
