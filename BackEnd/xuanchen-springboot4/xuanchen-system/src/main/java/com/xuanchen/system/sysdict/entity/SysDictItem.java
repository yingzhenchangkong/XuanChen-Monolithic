package com.xuanchen.system.sysdict.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->字典子表
 *
 * @author XuanChen
 * @date 2025-06-03
 */
@Data
public class SysDictItem implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    private String id;
    /**
     * 字典编码
     */
    private String dictCode;
    /**
     * 字典文本
     */
    private String dictItemText;
    /**
     * 字典值
     */
    private String dictItemValue;
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
    private Boolean delFlag;
    /**
     * 创建者
     */
    private String createBy;
    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /**
     * 更新者
     */
    private String updateBy;
    /**
     * 更新时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}