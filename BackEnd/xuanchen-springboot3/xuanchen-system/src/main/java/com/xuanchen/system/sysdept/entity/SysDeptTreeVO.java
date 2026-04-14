package com.xuanchen.system.sysdept.entity;

import java.util.List;

/**
 * 实体类 --> 部门树节点VO类
 *
 * @author XuanChen
 * @date 2025-11-13
 */
public class SysDeptTreeVO {
    private String title;
    private String key;
    private List<SysDeptTreeVO> children;

    // Constructors
    public SysDeptTreeVO() {
    }

    public SysDeptTreeVO(String title, String key) {
        this.title = title;
        this.key = key;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<SysDeptTreeVO> getChildren() {
        return children;
    }

    public void setChildren(List<SysDeptTreeVO> children) {
        this.children = children;
    }
}
