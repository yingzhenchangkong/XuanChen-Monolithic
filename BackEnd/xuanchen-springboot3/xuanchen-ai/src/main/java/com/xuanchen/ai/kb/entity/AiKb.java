package com.xuanchen.ai.kb.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->AI知识库
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Data
@TableName("ai_kb")
public class AiKb implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 知识库名称
     */
    private String kbName;

    /**
     * 知识库描述
     */
    private String kbDesc;

    /**
     * 向量化模型（入库时的模型，更换模型需重新向量化）
     */
    private String embedModel;

    /**
     * 分块大小（字符）
     */
    private Integer chunkSize;

    /**
     * 分块重叠（字符）
     */
    private Integer chunkOverlap;

    /**
     * 状态（1启用 0停用）
     */
    private Integer status;

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
