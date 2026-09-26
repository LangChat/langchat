package cn.langchat.common.oss.model;

/**
 * OSS 对象描述。
 *
 * @param objectKey 对象键
 * @param url 访问地址
 * @param platform 存储平台
 * @param size 文件大小
 * @author LangChat Team
 * @since 2026/3/24
 */
public record OssObject(
        String objectKey,
        String url,
        String platform,
        Long size
) {
}
