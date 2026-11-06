package com.xuanchen.generator.genprofile.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类 --> 代码生成项目预设
 * <p>
 * 把「数据源 + 输出目录 + 相对路径 + 包名/模块名 + 模板」打包成一份可复用的预设，
 * 新建表配置时选一下即可带出，避免每个项目重复填 6 个字段。
 *
 * @author XuanChen
 * @date 2026-10-20
 */
@Data
@TableName("gen_project_profile")
public class GenProjectProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;
    /**
     * 预设名称
     */
    private String profileName;
    /**
     * 默认数据源 ID
     */
    private String databaseId;
    /**
     * 输出目录
     */
    private String outputDir;
    /**
     * 后端相对路径
     */
    private String backendRelPath;
    /**
     * 前端相对路径
     */
    private String frontendRelPath;
    /**
     * 包名
     */
    private String packageName;
    /**
     * 模块名
     */
    private String moduleName;
    /**
     * 上级菜单 ID
     */
    private String parentMenuId;
    /**
     * 生成模板（crud / backend）
     */
    private String template;
    /**
     * 排序码
     */
    private Integer orderNo;
    /**
     * 删除状态（0正常，1已删除）
     */
    @TableLogic
    private Integer delFlag;
    /**
     * 创建者
     */
    private String createBy;
    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /**
     * 更新者
     */
    private String updateBy;
    /**
     * 更新时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
