package com.xuanchen.system.sysrolemenu.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 实体类-->角色 菜单 对应关系
 *
 * @author XuanChen
 * @date 2025-04-14
 */
@Data
public class SysRoleMenu implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    private String id;
    /**
     * 角色编码
     */
    private String roleId;
    /**
     * 菜单编码
     */
    private String menuId;
}
