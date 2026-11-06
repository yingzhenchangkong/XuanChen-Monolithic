package com.xuanchen.visualization.visscreen.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->大屏数据集（一条 SQL 对应一个数据集，组件按 datasetKey 引用）
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Data
@TableName("vis_dataset")
public class VisDataset implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 所属大屏ID
     */
    private String screenId;

    /**
     * 数据集编码（同一大屏内唯一，组件通过它引用）
     */
    private String datasetKey;

    /**
     * 数据集名称
     */
    private String datasetName;

    /**
     * 查询 SQL（只允许 SELECT/WITH，服务端做安全校验）
     */
    private String sqlText;

    /**
     * 外部数据源ID（为空或 null = 系统主库；其他值 = sys_datasource 表 id）
     */
    private String dsId;

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
