package cn.langchat.claw.auth.mapper;

import cn.langchat.claw.auth.entity.AigcUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联 Mapper。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Mapper
public interface AigcUserRoleMapper extends BaseMapper<AigcUserRole> {
}
