package com.xuanchen.filemanage.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 配置类-->文件中心存储配置（xuanchen.storage.*）
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Data
@Component
@ConfigurationProperties(prefix = "xuanchen.storage")
public class StorageProperties {

    /**
     * 存储类型：minio | local（local 为缺省兜底，保证未配置时行为与历史一致）
     */
    private String type = "local";

    /**
     * 通用附件默认桶（私有读）
     */
    private String defaultBucket = "xc-attach";

    /**
     * 公开资源桶（头像等，可 inline 预览）
     */
    private String publicBucket = "xc-public";

    private final Minio minio = new Minio();

    @Data
    public static class Minio {
        /**
         * MinIO 服务地址（S3 API 端口，默认 9000）
         */
        private String endpoint = "http://127.0.0.1:9000";

        private String accessKey = "minioadmin";

        private String secretKey = "minioadmin";

        /**
         * 预签名 URL 有效期（分钟）
         */
        private int presetExpiryMinutes = 30;
    }
}
