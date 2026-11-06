package com.xuanchen.ai.kb.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->AI知识库文档
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Data
@TableName("ai_document")
public class AiDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 所属知识库id
     */
    private String kbId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件中心 FileInfo.id（MinIO 对象经 IFileInfoService 上传生成）
     */
    private String fileId;

    /**
     * 文件类型（pdf/docx/xlsx/txt/md...）
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 内容指纹（32位小写MD5，冗余自文件中心 file_info；同知识库内容查重用）
     */
    private String md5;

    /**
     * 解析状态（pending待解析/parsing解析中/ready就绪/failed失败）
     */
    private String status;

    /**
     * 分块数量
     */
    private Integer chunkCount;

    /**
     * 解析失败原因
     */
    private String errorMsg;

    /**
     * 删除状态（0正常，1已删除）
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 创建者
     */
    private String createBy;
    @TableField(exist = false)
    private String createName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;
    @TableField(exist = false)
    private String updateName;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
