package com.xuanchen.system.sysmenu.entity;

import cn.idev.excel.annotation.format.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->菜单
 *
 * @author XuanChen
 * @date 2025-04-04
 */
@Data
public class SysMenu implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 菜单ID
     */
    private String id;
    /**
     * 父菜单ID
     */
    private String parentId;
    /**
     * 路由地址
     */
    private String path;
    /**
     * 路由名称
     */
    private String name;

    /**
     * 组件路径
     */
    private String component;
    /**
     * 权限标识
     */
    private String perms;
    /**
     * 标题
     */
    private String title;
    /**
     * 菜单图标
     */
    private String icon;
    /**
     * 类型（M目录 C菜单 F按钮）
     */
    private String menuType;
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
    private Boolean delFlag;
    /**
     * 创建者
     */
    private String createBy;
    /**
     * 创建时间
     */
    @DateTimeFormat("yyyy-MM-dd")
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
