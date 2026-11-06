package com.xuanchen.api.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->数据接口服务-接口定义
 * steps 为步骤链配置 JSON 数组，四类步骤：sql/http/map/save。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Data
@TableName("api")
public class Api implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花，字符串存储）
     */
    private String id;

    /**
     * 接口编码（访问路径 /open/api/{api_code}）
     */
    private String apiCode;

    /**
     * 接口名称
     */
    private String apiName;

    /**
     * 访问密钥（空=免鉴权，非空则请求须带 X-Api-Key 头或 apiKey 参数）
     */
    private String apiKey;

    /**
     * 状态（0草稿，1已发布，2已停用）
     */
    private Integer status;

    /**
     * 步骤链配置 JSON
     */
    private String steps;

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
