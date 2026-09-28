package com.xuanchen.system.sysuser.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->用户
 *
 * @author XuanChen
 * @date 2025-04-03
 */
@Data
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @ExcelIgnore
    private String id;
    /**
     * 用户名
     */
    @ExcelProperty("用户名")
    private String userName;
    /**
     * 昵称
     */
    @ExcelProperty("昵称")
    private String nickName;
    /**
     * 密码（BCrypt 哈希）：只允许入站反序列化（新增/重置密码提交），任何接口响应均不返回
     */
    @ExcelIgnore
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    /**
     * 是否要求下次登录修改密码（1是 0否）：
     * 新增/导入用户使用随机临时密码、管理员重置密码后置 1；用户本人修改密码后置 0。
     * 服务端字段，仅由服务端写入，不接受客户端赋值。
     */
    @ExcelIgnore
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer pwdResetRequired;
    /**
     * 手机号
     */
    @ExcelProperty("手机号")
    private String mobile;
    /**
     * 邮箱
     */
    @ExcelProperty("邮箱")
    private String email;
    /**
     * 头像
     */
    @ExcelIgnore
    private String avatar;
    /**
     * 账号状态（1正常，2冻结）
     */
    @ExcelProperty("账号状态")
    private Integer status;
    /**
     * 盐值：服务端内部字段，禁止出现在任何接口响应中
     */
    @ExcelIgnore
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String salt;
    /**
     * 删除状态（0正常，1已删除）
     */
    @ExcelProperty(value = "删除状态")
    @TableLogic
    private Integer delFlag;
    /**
     * 创建者
     */
    @ExcelIgnore
    private String createBy;
    /**
     * 创建时间
     */
    @ExcelIgnore
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /**
     * 更新者
     */
    @ExcelIgnore
    private String updateBy;
    /**
     * 更新时间
     */
    @ExcelIgnore
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 角色IDS
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String[] roleIds;
    /**
     * 部门IDS
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String[] deptIds;
    /**
     * 岗位IDS
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String[] postIds;
    /**
     * 文件列表
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String[] fileList;

}
