package com.xuanchen.system.sysaccount.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanchen.common.excel.annotation.ExcelDict;
import com.xuanchen.common.excel.converter.ExcelDictConverter;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.annotation.write.style.ColumnWidth;
import org.apache.fesod.sheet.annotation.write.style.ContentStyle;
import org.apache.fesod.sheet.enums.poi.HorizontalAlignmentEnum;
import org.apache.fesod.sheet.enums.poi.VerticalAlignmentEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->账号（仅 sys_account 表字段 + 只读的人员姓名展示）
 * <p>
 * 边界约定：账号管理只维护 sys_account 的列；人员姓名（personName）由 SQL 只读带出，
 * 不参与新增/编辑写入。人员档案的字段维护一律走人员管理（MdPersonController）。
 *
 * @author XuanChen
 * @date 2025-04-03
 */
@Data
public class SysAccountView implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @ExcelIgnore
    private String id;
    /**
     * 人员ID（关联 md_person.id，可为空=未绑定人员）
     */
    @ExcelIgnore
    private String personId;
    /**
     * 用户名
     */
    @ExcelProperty("用户名")
    @ColumnWidth(20)
    private String userName;
    /**
     * 昵称（系统内展示名）
     */
    @ExcelProperty("昵称")
    @ColumnWidth(18)
    private String nickName;
    /**
     * 头像（不参与导入导出）
     */
    @ExcelIgnore
    private String avatar;
    /**
     * 手机号（登录凭证 / 短信验证）
     */
    @ExcelProperty("手机号")
    @ColumnWidth(15)
    private String mobile;
    /**
     * 邮箱（找回密码 / 通知）
     */
    @ExcelProperty("邮箱")
    @ColumnWidth(26)
    private String email;
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
     * 绑定人员姓名（只读展示：md_person.nick_name，不参与任何写入）
     */
    @ExcelProperty("绑定人员")
    @ColumnWidth(20)
    private String personName;
    /**
     * 账号状态（1正常，2冻结）：导出翻译取自 DB 数据字典 user_status（字典管理可维护）
     */
    @ExcelProperty(value = "账号状态", converter = ExcelDictConverter.class)
    @ExcelDict("user_status")
    @ColumnWidth(15)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
    private Integer status;
    /**
     * 删除状态（0正常，1已删除）：导出翻译取自 DB 数据字典 del_flag（字典管理可维护）
     */
    @ExcelProperty(value = "删除状态", converter = ExcelDictConverter.class)
    @ExcelDict("del_flag")
    @TableLogic
    @ColumnWidth(15)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
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
     * 角色IDS（账号维度：sys_account_role.account_id）
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String[] roleIds;
    /**
     * 文件列表
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String[] fileList;

}
