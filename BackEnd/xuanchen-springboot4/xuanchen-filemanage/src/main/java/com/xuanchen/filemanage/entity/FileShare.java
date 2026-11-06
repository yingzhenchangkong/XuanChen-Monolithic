package com.xuanchen.filemanage.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->文件共享（二期）
 * 公开链接凭 share_no（+ 可选提取码）访问 /share/info、/share/download，
 * 失效条件：撤销（del_flag=1）/ 过期 / 源文件被删除。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Data
@TableName("file_share")
public class FileShare implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花，字符串存储）
     */
    private String id;

    /**
     * 分享编号（公开链接使用，随机UUID）
     */
    private String shareNo;

    /**
     * 文件id（file_info.id）
     */
    private String fileId;

    /**
     * 分享人登录名
     */
    private String username;

    /**
     * 提取码（空=无需提取码）
     */
    private String shareCode;

    /**
     * 失效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;

    /**
     * 下载次数
     */
    private Integer visitCount;

    /**
     * 撤销状态（0有效，1已撤销）
     */
    private Integer delFlag;

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
