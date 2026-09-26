package cn.langchat.claw.aigc.biz.entity;

import cn.langchat.claw.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 向量库配置实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_vector_store")
@EqualsAndHashCode(callSuper = true)
public class AigcVectorStore extends BaseDO {

    /** 向量库主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** 向量库名称。 */
    private String name;
    /** 向量库供应商。 */
    private String provider;
    /** 主机地址。 */
    private String host;
    /** 端口。 */
    private Integer port;
    /** 用户名。 */
    private String username;
    /** 密码。 */
    private String password;
    /** 数据库名称。 */
    private String databaseName;
    /** 向量维度。 */
    private Integer dimension;
    /** 表名或集合名。 */
    private String tableName;
}
