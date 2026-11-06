package com.xuanchen.filemanage.storage;

import java.io.InputStream;

/**
 * 存储抽象接口-->文件中心统一存储入口
 * 业务代码只依赖本接口，不感知底层是 MinIO 还是本地磁盘；
 * 由 xuanchen.storage.type 配置决定装配 MinioStorageService 或 LocalStorageService。
 *
 * <p>约定：数据库（file_info）只存 bucket + objectKey，URL 一律按需生成，禁止落库。</p>
 *
 * @author XuanChen
 * @date 2026-10-25
 */
public interface StorageService {

    /**
     * 上传对象
     *
     * @param bucket      桶名
     * @param objectKey   存储键（服务端生成：bizType/yyyyMM/雪花_原名.ext）
     * @param in          内容流（由调用方负责关闭）
     * @param size        字节数
     * @param contentType MIME 类型，可为 null
     * @throws StorageException 存储端不可用或写入失败
     */
    void put(String bucket, String objectKey, InputStream in, long size, String contentType);

    /**
     * 打开对象读取流（调用方负责关闭）
     *
     * @throws StorageException 对象不存在或读取失败
     */
    InputStream get(String bucket, String objectKey);

    /**
     * 生成预览/下载地址
     * MinIO 实现返回带有效期的预签名 URL；本地实现返回 /filemanage/static/** 内部地址。
     *
     * @param inline      true=浏览器内联预览，false=附件下载
     * @param contentType 对象 MIME 类型（可为 null）：文本类需据此补 charset，否则浏览器按默认编码渲染会乱码
     */
    String previewUrl(String bucket, String objectKey, String downloadName, boolean inline, String contentType);

    /**
     * 删除对象（对象不存在时静默成功）
     */
    void remove(String bucket, String objectKey);

    /**
     * 对象是否存在
     */
    boolean exists(String bucket, String objectKey);

    /**
     * 存储端异常统一包装，业务层据此转换为友好提示
     */
    class StorageException extends RuntimeException {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
