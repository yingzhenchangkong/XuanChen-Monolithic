package com.xuanchen.filemanage.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->文件中心用户配额（二期）
 * 未配置记录的用户使用 xuanchen.fileinfo.default-quota-bytes 默认配额。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Data
@TableName("file_quota")
public class FileQuota implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花，字符串存储）
     */
    private String id;

    /**
     * 登录名（唯一）
     */
    private String username;

    /**
     * 配额上限（字节，0=不限制）
     */
    private Long quotaBytes;

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
