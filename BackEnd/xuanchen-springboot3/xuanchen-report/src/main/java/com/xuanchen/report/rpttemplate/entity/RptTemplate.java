package com.xuanchen.report.rpttemplate.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->报表模板
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Data
public class RptTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 模板编码（全库唯一）
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板类型（1明细表 2汇总表 3自由格式）
     */
    private Integer templateType;

    /**
     * 模板配置 JSON（Univer snapshot + 参数定义）
     */
    private String configJson;

    /**
     * 状态（1启用 0停用）
     */
    private Integer status;

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
