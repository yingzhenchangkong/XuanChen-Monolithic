package com.xuanchen.filemanage.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->文件中心文件夹
 * 层级模型：parent_id='0' 为根目录；删除为逻辑删（文件夹本身不进回收站，
 * 其下文件随删除动作整体移入回收站）。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Data
@TableName("file_folder")
public class FileFolder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花，字符串存储）
     */
    private String id;

    /**
     * 文件夹名
     */
    private String folderName;

    /**
     * 父文件夹id（0=根目录）
     */
    private String parentId;

    /**
     * 归属人登录名
     */
    private String username;

    /**
     * 删除状态（0正常，1已删除）
     */
    @TableLogic
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
