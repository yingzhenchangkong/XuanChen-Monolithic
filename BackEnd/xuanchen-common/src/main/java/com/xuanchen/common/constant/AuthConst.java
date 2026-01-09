package com.xuanchen.common.constant;

/**
 * 常量-->认证、授权
 *
 * @author XuanChen
 * @date 2025-03-24
 */
public interface AuthConst {
    /**
     * 数据令牌
     */
    String XC_ACCESS_TOKEN = "XC-ACCESS-TOKEN";
    /**
     * 登录失败次数的键的前缀
     */
    String PREFIX_LOGIN_FAIL_TIMES = "PREFIX_LOGIN_FAIL_TIMES_";
    /**
     * 登录用户令牌的键的前缀
     */
    String PREFIX_USER_TOKEN = "PREFIX_USER_TOKEN_";
}
