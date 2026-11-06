package com.xuanchen.report.rptreport.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->报表
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Data
public class RptReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 报表编码（查看页路由用，全库唯一）
     */
    private String reportCode;

    /**
     * 报表名称
     */
    private String reportName;

    /**
     * 报表类型（1明细表 2汇总表 3自由格式）
     */
    private Integer reportType;

    /**
     * 来源模板ID（新增时选择，模板快照在创建时复制到 configJson，之后与模板解耦）
     */
    private String templateId;

    /**
     * 来源模板名称（列表展示用，非表字段）
     */
    @TableField(exist = false)
    private String templateName;

    /**
     * 报表配置 JSON（Univer snapshot + 参数定义 + 单元格绑定关系）
     */
    private String configJson;

    /**
     * 状态（0草稿 1已发布 2停用）
     */
    private Integer status;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTime;

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
