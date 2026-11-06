package com.xuanchen.businessprocess.definition.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->审批流程定义
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
@TableName("bpm_definition")
public class BpmDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 流程名称
     */
    private String name;

    /**
     * 流程编码（唯一）
     */
    private String code;

    /**
     * 业务类型（业务模块接入标识）
     */
    private String businessType;

    /**
     * 当前版本号
     */
    private Integer version;

    /**
     * 状态（0草稿 1启用 2停用）
     */
    private Integer status;

    /**
     * BPMN 2.0 XML（设计器回显/渲染高亮）
     */
    private String bpmnXml;

    /**
     * 绑定的在线表单ID（可空=不使用在线表单，走通用字段表单）
     */
    private String onlineFormId;

    /**
     * 备注
     */
    private String remark;

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
