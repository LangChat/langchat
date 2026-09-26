package cn.langchat.common.persistence;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

/**
 * 实体公共审计字段基类。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
public class BaseDO {

    /** 创建人。 */
    @TableField(fill = FieldFill.INSERT)
    private String creator;

    /** 更新人。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updater;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private Long createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateTime;
}
