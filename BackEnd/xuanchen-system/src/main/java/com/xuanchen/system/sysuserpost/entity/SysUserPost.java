package com.xuanchen.system.sysuserpost.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * 实体类-->用户 岗位 对应关系
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@Data
public class SysUserPost implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    private String id;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 用户名
     */
    @TableField(exist = false)
    private String userName;
    /**
     * 昵称
     */
    @TableField(exist = false)
    private String nickName;
    /**
     * 岗位ID
     */
    private String postId;
    /**
     * 岗位编码
     */
    @TableField(exist = false)
    private String postCode;
    /**
     * 岗位名称
     */
    @TableField(exist = false)
    private String postName;
}