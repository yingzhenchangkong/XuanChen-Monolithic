package com.xuanchen.system.sysuserrole.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * 实体类-->用户 角色 对应关系
 *
 * @author XuanChen
 * @date 2025-04-14
 */
@Data
public class SysUserRole implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    private String id;
    /**
     * 用户编码
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
     * 手机号
     */
    @TableField(exist = false)
    private String mobile;
    /**
     * 邮箱
     */
    @TableField(exist = false)
    private String email;
    /**
     * 角色编码
     */
    private String roleId;
    /**
     * 角色编码
     */
    @TableField(exist = false)
    private String roleCode;
    /**
     * 角色名称
     */
    @TableField(exist = false)
    private String roleName;
    /**
     * 角色描述
     */
    @TableField(exist = false)
    private String roleDescription;
}