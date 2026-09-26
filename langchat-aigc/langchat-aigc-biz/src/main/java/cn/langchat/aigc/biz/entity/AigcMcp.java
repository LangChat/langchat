package cn.langchat.aigc.biz.entity;

import cn.langchat.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP 配置实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_mcp")
@EqualsAndHashCode(callSuper = true)
public class AigcMcp extends BaseDO {

    /** MCP 主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** UUID。 */
    private String uuid;
    /** 服务名称。 */
    private String name;
    /** MCP 配置 JSON。 */
    private String mcpJson;
    /** 封面地址。 */
    private String coverUrl;
    /** 标签。 */
    private String tags;
    /** 是否已授权。 */
    private Boolean authorized;
    /** 协议类型。 */
    private String transport;
    /** SSE 地址。 */
    private String sseUrl;
    /** 请求头。 */
    private String headers;
    /** Docker 镜像。 */
    private String dockerImage;
    /** Docker Host。 */
    private String dockerHost;
    /** 站点地址。 */
    private String siteUrl;
    /** 超时时间。 */
    private Integer timeout;
    /** 描述信息。 */
    private String description;
}
