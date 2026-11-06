package com.xuanchen.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->数据接口服务-采集数据
 * save 步骤 / 定时采集的落库结果，data 为单条采集记录 JSON。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Data
@TableName("api_data")
public class ApiData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花，字符串存储）
     */
    private String id;

    /**
     * 来源接口编码
     */
    private String apiCode;

    /**
     * 采集数据 JSON
     */
    private String data;

    /**
     * 采集时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
