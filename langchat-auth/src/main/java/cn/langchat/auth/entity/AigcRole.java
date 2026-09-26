package cn.langchat.auth.entity;

import cn.langchat.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_role")
@EqualsAndHashCode(callSuper = true)
public class AigcRole extends BaseDO {

    /** 角色主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** 角色名称。 */
    private String name;
    /** 角色编码。 */
    private String code;
    /** 角色描述。 */
    private String description;
}
