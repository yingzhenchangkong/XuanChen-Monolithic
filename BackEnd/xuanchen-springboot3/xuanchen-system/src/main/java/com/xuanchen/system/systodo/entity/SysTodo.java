package com.xuanchen.system.systodo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类 --> 系统待办
 *
 * <p>user_name 存 sys_account.user_name，与 sys_notice_status.user_id 的既有语义一致
 * （按登录名归属，展示昵称时 JOIN sys_account）。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
@TableName("sys_todo")
public class SysTodo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 所属用户（sys_account.user_name）
     */
    private String userName;

    /**
     * 待办标题
     */
    private String title;

    /**
     * 待办内容
     */
    private String content;

    /**
     * 优先级（1高 2中 3低）
     */
    private Integer priority;

    /**
     * 截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime dueDate;

    /**
     * 状态（0待办 1完成）
     */
    private Integer status;

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime finishTime;

    /**
     * 创建人（MybatisInterceptor 自动填充）
     */
    private String createBy;

    /**
     * 创建时间（MybatisInterceptor 自动填充）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新人（MybatisInterceptor 自动填充）
     */
    private String updateBy;

    /**
     * 更新时间（MybatisInterceptor 自动填充）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
