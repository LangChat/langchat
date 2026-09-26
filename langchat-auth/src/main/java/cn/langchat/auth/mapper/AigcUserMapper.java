package cn.langchat.auth.mapper;

import cn.langchat.auth.entity.AigcUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Mapper
public interface AigcUserMapper extends BaseMapper<AigcUser> {
}
