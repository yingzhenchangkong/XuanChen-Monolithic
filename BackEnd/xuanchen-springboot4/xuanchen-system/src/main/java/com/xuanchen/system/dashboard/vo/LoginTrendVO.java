package com.xuanchen.system.dashboard.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * VO-->工作台登录趋势（按天）
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
public class LoginTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日期（yyyy-MM-dd）
     */
    private String date;

    /**
     * 成功登录次数
     */
    private Long count;
}
