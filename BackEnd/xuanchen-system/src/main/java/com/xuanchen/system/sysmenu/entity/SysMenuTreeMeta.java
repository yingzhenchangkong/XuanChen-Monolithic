package com.xuanchen.system.sysmenu.entity;

import java.io.Serializable;
/**
 * 实体类-->菜单树下级
 *
 * @author XuanChen
 * @date 2025-05-05
 */
public class SysMenuTreeMeta implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 标题
     */
    private String title;
    /**
     * 图标
     */
    private String icon;

    public SysMenuTreeMeta() {
    }

    public SysMenuTreeMeta(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}