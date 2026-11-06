package com.xuanchen.api.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->数据接口服务-调用日志
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Data
@TableName("log_api")
public class LogApi implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花，字符串存储）
     */
    private String id;

    /**
     * 接口编码
     */
    private String apiCode;

    /**
     * 触发方式（1手动调用，2定时采集，3测试运行）
     */
    private Integer triggerType;

    /**
     * 是否成功（0失败，1成功）
     */
    private Integer success;

    /**
     * 耗时（毫秒）
     */
    private Integer costMs;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 调用时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 接口名称（按 api_code 关联 api 表回填，非数据库字段）
     */
    @TableField(exist = false)
    private String apiName;

    /**
     * 开始时间（调用时间区间查询，非数据库字段）
     */
    @TableField(exist = false)
    private String beginTime;

    /**
     * 结束时间（调用时间区间查询，非数据库字段）
     */
    @TableField(exist = false)
    private String endTime;
}
