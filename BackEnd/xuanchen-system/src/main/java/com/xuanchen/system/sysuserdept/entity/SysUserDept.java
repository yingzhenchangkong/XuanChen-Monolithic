package com.xuanchen.system.sysuserdept.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 实体类-->用户 部门 对应关系
 *
 * @author XuanChen
 * @date 2025-12-04
 */
@Data
public class SysUserDept implements Serializable {
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
     * 部门ID
     */
    private String deptId;
    /**
     * 部门编码
     */
    @TableField(exist = false)
    private String deptCode;
    /**
     * 部门名称
     */
    @TableField(exist = false)
    private String deptName;
    /**
     * 用户(多)
     */
    @TableField(exist = false)
    private List<String> listUser;
}
