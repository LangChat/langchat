package cn.langchat.claw.aigc.biz.entity;

import cn.langchat.claw.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资源文件实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_oss")
@EqualsAndHashCode(callSuper = true)
public class AigcOss extends BaseDO {

    /** 资源主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** 文件存储名称。 */
    private String filename;
    /** 原始文件名。 */
    private String originalFilename;
    /** 文件访问地址。 */
    private String url;
    /** 文件绝对路径。 */
    private String path;
    /** 文件大小。 */
    private Integer size;
    /** 文件后缀。 */
    private String ext;
    /** 文件内容类型。 */
    private String contentType;
    /** 存储平台。 */
    private String platform;
}
