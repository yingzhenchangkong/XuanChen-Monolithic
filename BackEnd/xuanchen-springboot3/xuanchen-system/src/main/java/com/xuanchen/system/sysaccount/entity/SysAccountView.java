package com.xuanchen.system.sysaccount.entity;

import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanchen.common.excel.annotation.ExcelDict;
import com.xuanchen.common.excel.converter.ExcelDictConverter;
import lombok.Data;

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
    private String userName;
    /**
     * 昵称（系统内展示名）
     */
    @ExcelProperty("昵称")
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
    private String mobile;
    /**
     * 邮箱（找回密码 / 通知）
     */
    @ExcelProperty("邮箱")
    private String email;
    /**
     * 密码
     * <p>
     * WRITE_ONLY：新增/重置密码等入参仍可反序列化接收，但任何 JSON 响应（列表/详情/下拉/回收站）
     * 都不会把密码哈希下发前端；Excel 导出同样忽略。
     */
    @ExcelIgnore
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    /**
     * 是否要求下次登录修改密码（1是0否）。
     * 仅服务端按业务流程置位（新增随机临时口令/导入/管理员重置=1，本人改密=0），
     * 不接受客户端在 add/edit 中直接赋值；不下发到 Excel。
     */
    @ExcelIgnore
    private Integer pwdResetRequired;
    /**
     * 绑定人员姓名（只读展示：md_person.nick_name，不参与任何写入）
     */
    @ExcelProperty("绑定人员")
    private String personName;
    /**
     * 账号状态（1正常，2冻结）：导出翻译取自 DB 数据字典 user_status（字典管理可维护）
     */
    @ExcelProperty(value = "账号状态", converter = ExcelDictConverter.class)
    @ExcelDict("user_status")
    private Integer status;
    /**
     * 盐值
     * <p>
     * 盐值仅服务端持有：不通过任何 JSON 响应下发，也不接受客户端赋值（新增时服务端重新生成）。
     */
    @ExcelIgnore
    @JsonIgnore
    private String salt;
    /**
     * 删除状态（0正常，1已删除）：导出翻译取自 DB 数据字典 del_flag（字典管理可维护）
     */
    @ExcelProperty(value = "删除状态", converter = ExcelDictConverter.class)
    @ExcelDict("del_flag")
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
