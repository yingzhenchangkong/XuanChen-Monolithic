package com.xuanchen.online.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->在线表单（流程审批）
 * 数据模型：前端表单设计器产出的字段列表 JSON 整体落库（schema_json）；
 * 可被流程定义（bpm_definition.online_form_id）可选绑定，发起申请时动态渲染。
 *
 * @author XuanChen
 * @date 2026-10-28
 */
@Data
@TableName("online_form")
public class OnlineForm implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花，字符串存储）
     */
    private String id;

    /**
     * 表单名称
     */
    private String formName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 表单设计 JSON（字段列表：type/label/key/placeholder/required/options 等）
     */
    private String schemaJson;

    /**
     * 删除状态（0正常，1已删除）
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 创建人（归属人）
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
