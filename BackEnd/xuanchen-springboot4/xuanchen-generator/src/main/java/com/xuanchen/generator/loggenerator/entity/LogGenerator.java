package com.xuanchen.generator.loggenerator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类 --> 代码生成记录
 *
 * @author XuanChen
 * @date 2026-10-19
 */
@Data
@TableName("log_generator")
public class LogGenerator implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private String id;
    /** 表配置ID */
    private String tableId;
    /** 物理表名 */
    private String tableName;
    /** 表注释 */
    private String tableComment;
    /** 模块名 */
    private String moduleName;
    /** 包名 */
    private String packageName;
    /** 生成方式：DISK 落盘 / ZIP 下载 / PREVIEW 预览 */
    private String generateType;
    /** 使用的模板 */
    private String template;
    /** 文件数 */
    private Integer fileCount;
    /** 文件清单（换行分隔） */
    private String fileList;
    /** 是否成功 */
    private Boolean success;
    /** 结果说明/失败原因 */
    private String message;
    /** 操作人 */
    private String createBy;
    /** 生成时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 开始时间（生成时间区间查询，非数据库字段） */
    @TableField(exist = false)
    private String beginTime;

    /** 结束时间（生成时间区间查询，非数据库字段） */
    @TableField(exist = false)
    private String endTime;
}
