package cn.langchat.aigc.biz.entity;

import cn.langchat.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统日志实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_log")
@EqualsAndHashCode(callSuper = true)
public class AigcLog extends BaseDO {

    /** 日志主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** 日志类型。 */
    private String type;
    /** 操作用户名。 */
    private String username;
    /** 操作描述。 */
    private String operation;
    /** 请求地址。 */
    private String url;
    /** 耗时毫秒。 */
    private Long time;
    /** 调用方法。 */
    private String method;
    /** 请求参数。 */
    private String params;
    /** IP 地址。 */
    private String ip;
    /** 用户代理。 */
    private String userAgent;
}
