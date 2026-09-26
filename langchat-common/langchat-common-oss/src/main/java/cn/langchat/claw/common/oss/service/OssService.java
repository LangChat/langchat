package cn.langchat.claw.common.oss.service;

import cn.langchat.claw.common.oss.model.OssObject;
import cn.langchat.claw.common.oss.model.OssUploadRequest;

/**
 * OSS 存储服务接口。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface OssService {

    /**
     * 上传文件。
     */
    OssObject upload(OssUploadRequest request);

    /**
     * 下载文件内容。
     *
     * @param objectKey 对象键
     * @return 文件字节内容
     */
    byte[] download(String objectKey);

    /**
     * 删除文件。
     */
    boolean delete(String objectKey);
}
