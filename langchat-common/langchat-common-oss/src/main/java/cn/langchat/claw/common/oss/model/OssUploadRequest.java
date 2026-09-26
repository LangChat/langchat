package cn.langchat.claw.common.oss.model;

import java.util.Map;

/**
 * OSS 上传请求。
 *
 * @param filename 文件名
 * @param contentType 内容类型
 * @param bytes 文件字节数组
 * @param metadata 扩展元数据
 * @author LangChat Team
 * @since 2026/3/24
 */
public record OssUploadRequest(
        String filename,
        String contentType,
        byte[] bytes,
        Map<String, String> metadata
) {
}
