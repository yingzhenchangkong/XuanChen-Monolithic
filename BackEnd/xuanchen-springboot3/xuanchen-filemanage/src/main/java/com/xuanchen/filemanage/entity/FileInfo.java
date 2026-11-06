package com.xuanchen.filemanage.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->文件元数据（文件中心）
 * 数据库只存 bucket + objectKey，URL 一律按需生成，禁止落库。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Data
@TableName("file_info")
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花，字符串存储）
     */
    private String id;

    /**
     * 存储桶名
     */
    private String bucket;

    /**
     * 存储键：bizType/yyyyMM/雪花_原名.ext
     */
    private String objectKey;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 扩展名（小写，jpeg 归一为 jpg）
     */
    private String ext;

    /**
     * MIME 类型
     */
    private String contentType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 存储类型（0本地 1MinIO）
     */
    private Integer storageType;

    /**
     * 业务类型：avatar/photo/common/convert...
     */
    private String bizType;

    /**
     * 上传人登录名
     */
    private String username;

    /**
     * 内容 MD5（二期秒传去重用）
     */
    private String md5;

    /**
     * 文件夹id（file_folder.id，空=根目录）
     */
    private String folderId;

    /**
     * 删除状态（0正常，1已删除=回收站）
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 删除时间（进回收站时记录，超期由定时任务彻底清理）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deleteTime;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
