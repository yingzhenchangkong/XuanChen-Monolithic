package com.xuanchen.filemanage.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->文件协作成员
 * <p>文件属主（file_info.username）之外的可访问人：permission 1=可编辑 2=只读。
 * 供 ONLYOFFICE 协同编辑的权限放开（OfficeEditService）与在线文档列表联查使用。</p>
 *
 * @author XuanChen
 * @date 2026-10-28
 */
@Data
@TableName("file_collaborator")
public class FileCollaborator implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花，字符串存储）
     */
    private String id;

    /**
     * 文件id（file_info.id）
     */
    private String fileId;

    /**
     * 协作者登录名
     */
    private String username;

    /**
     * 协作权限（1=可编辑，2=只读）
     */
    private Integer permission;

    /**
     * 创建者（文件属主）
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
