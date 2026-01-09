package com.xuanchen.system.sysmenu.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 实体类-->菜单树
 *
 * @author XuanChen
 * @date 2025-04-06
 */
public class SysMenuTree implements Serializable {
    private static final long serialVersionUID = 1L;

    private String key;
    private String title;
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
     * Meta
     */
    private SysMenuTreeMeta meta;
    /**
     * 子集
     */
    private List<SysMenuTree> children;
    /**
     * 权限标识
     */
    private String perms;
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


    public SysMenuTree() {

    }

    public SysMenuTree(SysMenu sysMenu) {
        this.key = sysMenu.getId();
        this.title = sysMenu.getTitle();
        this.id = sysMenu.getId();
        this.parentId = sysMenu.getParentId();
        this.path = sysMenu.getPath();
        this.name = sysMenu.getName();
        this.meta = new SysMenuTreeMeta(sysMenu.getTitle(), sysMenu.getIcon());
        this.component = sysMenu.getComponent();
        this.perms = sysMenu.getPerms();
        this.menuType = sysMenu.getMenuType();
        this.isLeaf = sysMenu.getIsLeaf();
        this.orderNo = sysMenu.getOrderNo();
        this.status = sysMenu.getStatus();
        if (!sysMenu.getIsLeaf()) {
            this.children = new ArrayList<>();
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public SysMenuTreeMeta getMeta() {
        return meta;
    }

    public void setMeta(SysMenuTreeMeta meta) {
        this.meta = meta;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public Boolean getIsLeaf() {
        return isLeaf;
    }

    public void setIsLeaf(Boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public List<SysMenuTree> getChildren() {
        return children;
    }

    public void setChildren(List<SysMenuTree> children) {
        this.children = children;
    }
}
