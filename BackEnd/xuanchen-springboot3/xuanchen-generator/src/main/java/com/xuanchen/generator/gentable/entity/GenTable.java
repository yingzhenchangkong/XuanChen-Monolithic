package com.xuanchen.generator.gentable.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类 --> 数据库表
 *
 * @author XuanChen
 * @date 2026-02-25
 */
@Data
@TableName("gen_table")
public class GenTable implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @ExcelIgnore
    private String id;
    /**
     * 数据库ID
     */
    @ExcelProperty(value = "数据库ID")
    private String databaseId;
    /**
     * 数据库表名
     */
    @ExcelProperty(value = "数据库表名")
    private String tableName;
    /**
     * 是否创建表（true：目标库中尚无该表，字段由 gen_table_column 配置，生成前先按配置建表）
     * <p>
     * 用 Boolean 而不是 Integer：前端开关直接提交 true/false，
     * sb3 其余整型布尔列保持原样，但本字段是新加的，与 sb4 保持同一 JSON 契约。
     */
    @ExcelProperty(value = "是否创建表")
    private Boolean ifCreateTable;
    /**
     * 数据库表注释
     */
    @ExcelProperty(value = "数据库表注释")
    private String tableComment;
    /**
     * 上级菜单ID（menu.sql 中菜单的 parent_id，0 表示根菜单）
     */
    @ExcelProperty(value = "上级菜单ID")
    private String parentMenuId;
    /**
     * 关联子表的表名
     */
    @ExcelProperty(value = "关联子表的表名")
    private String subTableName;
    /**
     * 子表关联的外键名
     */
    @ExcelProperty(value = "子表关联的外键名")
    private String subTableFkName;
    /**
     * 使用的模板
     */
    @ExcelProperty(value = "使用的模板")
    private String template;
    /**
     * 输出目录
     */
    @ExcelProperty(value = "输出目录")
    private String outputDir;
    /**
     * 后端输出相对路径（相对输出目录）：
     * 为空时直接输出到输出目录根下（com/xuanchen/...）；
     * 配置后输出到 输出目录/该相对路径 下，可直接指向仓库的 src/main/java。
     */
    @ExcelProperty(value = "后端输出相对路径")
    private String backendRelPath;
    /**
     * 前端输出相对路径（相对输出目录）：
     * 为空时输出到 输出目录/vue 下；配置后直接输出到该相对路径（如 FrontEnd/xuanchen-vue3/src/views）。
     */
    @ExcelProperty(value = "前端输出相对路径")
    private String frontendRelPath;
    /**
     * 包名
     */
    @ExcelProperty(value = "包名")
    private String packageName;
    /**
     * 模块名
     */
    @ExcelProperty(value = "模块名")
    private String moduleName;
    /**
     * 排序码
     */
    @ExcelIgnore
    private Integer orderNo;
    /**
     * 状态（1启用，0停用）
     */
    @ExcelIgnore
    private Integer status;
    /**
     * 删除状态（0正常，1已删除）
     */
    @ExcelIgnore
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
}
