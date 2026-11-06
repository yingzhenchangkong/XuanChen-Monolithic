package com.xuanchen.visualization.visscreen.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->数据大屏
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Data
@TableName("vis_screen")
public class VisScreen implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 大屏编码（访问地址用，全库唯一）
     */
    private String screenCode;

    /**
     * 大屏名称
     */
    private String screenName;

    /**
     * 大屏配置 JSON（画布 + 组件布局 + 组件样式 + 数据映射）
     */
    private String configJson;

    /**
     * 缩略图（相对路径，列表/画廊展示用）
     */
    private String thumbnail;

    /**
     * 状态（0草稿 1已发布 2停用）
     */
    private Integer status;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTime;

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
     * 创建者
     */
    private String createBy;
    @TableField(exist = false)
    private String createName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;
    @TableField(exist = false)
    private String updateName;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
