package com.xuanchen.report.rptdataset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;

/**
 * 实体类-->报表数据集
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Data
public class RptDataset implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 所属模板ID（设计整体在「模板设计」，数据集随模板复用）
     */
    private String templateId;

    /**
     * 引用数据源ID（为空 = 使用系统主库）
     */
    private String dsId;

    /**
     * 数据集编码（模板内唯一，供单元格绑定引用）
     */
    private String datasetCode;

    /**
     * 数据集名称
     */
    private String datasetName;

    /**
     * 查询 SQL（仅允许 SELECT，执行前经 SqlGuard 校验）
     */
    private String sqlText;

    /**
     * 参数定义 JSON：[{"code":"..","label":"..","type":"string|number|date|dict","dictType":"..","defaultValue":"..","required":true}]
     */
    private String paramsJson;

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
    private java.time.LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private java.time.LocalDateTime updateTime;

    /**
     * 数据源名称（列表展示）
     */
    @TableField(exist = false)
    private String dsName;
}
