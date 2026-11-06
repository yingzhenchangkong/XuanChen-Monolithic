package com.xuanchen.masterdata.mddept.entity;

import java.util.List;

/**
 * 实体类 --> 部门树节点VO类
 *
 * @author XuanChen
 * @date 2025-11-13
 */
public class MdDeptTreeVO {
    private String title;
    private String key;
    private List<MdDeptTreeVO> children;

    // Constructors
    public MdDeptTreeVO() {
    }

    public MdDeptTreeVO(String title, String key) {
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

    public List<MdDeptTreeVO> getChildren() {
        return children;
    }

    public void setChildren(List<MdDeptTreeVO> children) {
        this.children = children;
    }
}
