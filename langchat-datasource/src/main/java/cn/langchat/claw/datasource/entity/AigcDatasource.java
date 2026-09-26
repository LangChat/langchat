package cn.langchat.claw.datasource.entity;

import cn.langchat.claw.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据源实体。
 *
 * @author LangChat Team
 * @since 2026/8/27
 */
@Data
@TableName("aigc_datasource")
@EqualsAndHashCode(callSuper = true)
public class AigcDatasource extends BaseDO {

    /** 数据源主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 数据源名称。 */
    private String name;

    /** 数据库类型：MYSQL / POSTGRESQL / ORACLE / SQLSERVER。 */
    private String dbType;

    /** 主机地址。 */
    private String host;

    /** 端口。 */
    private Integer port;

    /** 数据库名。 */
    private String databaseName;

    /** 连接 URL，由数据库类型、主机、端口和库名自动生成。 */
    private String url;

    /** 用户名。 */
    private String username;

    /** 密码。 */
    private String password;

    /** Schema 名称（Oracle / PostgreSQL 用）。 */
    private String schemaName;

    /** 自定义表结构 JSON（原始结构复制后可编辑字段中文解释）。 */
    private String structureJson;

    /** 备注。 */
    private String remark;

    /** 是否启用。 */
    private Boolean enabled;
}
