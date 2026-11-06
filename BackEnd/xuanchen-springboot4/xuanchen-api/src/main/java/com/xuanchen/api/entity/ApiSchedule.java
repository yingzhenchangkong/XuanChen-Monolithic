package com.xuanchen.api.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->数据接口服务-定时采集配置
 * 调度器按 next_run_time 扫描执行，执行后依据 cron 推算下一次时间。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Data
@TableName("api_schedule")
public class ApiSchedule implements Serializable {

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
     * cron 表达式（Spring 6 位格式）
     */
    private String cron;

    /**
     * 是否启用（0停用，1启用）
     */
    private Integer enabled;

    /**
     * 上次执行时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastRunTime;

    /**
     * 下次执行时间（到点即被调度器扫描执行）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime nextRunTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除状态（0正常，1已删除）
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
