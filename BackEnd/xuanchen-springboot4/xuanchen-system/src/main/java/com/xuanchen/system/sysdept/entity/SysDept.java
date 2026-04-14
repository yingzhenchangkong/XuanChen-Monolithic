package com.xuanchen.system.sysdept.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类 --> 部门
 *
 * @author XuanChen
 * @date 2025-11-12
 */
@Data
public class SysDept implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 部门编码
     */
    private String deptCode;

    /**
     * 父部门编码
     */
    private String parentDeptCode;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 是否叶子节点（1是，0不是）
     */
    private Boolean isLeaf;

    /**
     * 排序码
     */
    private Integer orderNo;

    /**
     * 状态（1启用，0停用）
     */
    private Boolean status;

    /**
     * 删除状态（0正常，1已删除）
     */
    @TableLogic
    private Boolean delFlag;

    /**
     * 创建人
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
