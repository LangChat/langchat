package cn.langchat.claw.auth.entity;

import cn.langchat.claw.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_user")
@EqualsAndHashCode(callSuper = true)
public class AigcUser extends BaseDO {

    /** 用户主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** 用户名。 */
    private String username;
    /** 登录密码。 */
    private String password;
    /** 真实姓名。 */
    private String realName;
    /** 性别。 */
    private String sex;
    /** 手机号。 */
    private String phone;
    /** 邮箱。 */
    private String email;
    /** 头像。 */
    private String avatar;
    /** 状态。 */
    private Integer status;
    /** 部门 ID。 */
    private String deptId;
}
