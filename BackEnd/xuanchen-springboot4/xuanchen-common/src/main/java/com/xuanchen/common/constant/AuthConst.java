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
     * 登录失败次数的键的前缀（按用户名维度，保护账号不被针对爆破）
     */
    String PREFIX_LOGIN_FAIL_TIMES = "PREFIX_LOGIN_FAIL_TIMES_";
    /**
     * 登录失败次数的键的前缀（按来源 IP 维度，防止轮换用户名撞库/枚举）
     */
    String PREFIX_LOGIN_FAIL_TIMES_IP = "PREFIX_LOGIN_FAIL_TIMES_IP_";
    /**
     * 登录失败计数窗口（秒）：10 分钟滑动窗口
     */
    int LOGIN_FAIL_WINDOW_SECONDS = 600;
    /**
     * 同一用户名在窗口内允许的最大失败次数，达到即临时锁定登录
     */
    int LOGIN_MAX_FAIL_BY_USERNAME = 5;
    /**
     * 同一来源 IP 在窗口内允许的最大失败次数（跨所有用户名），
     * 阈值略高于用户名维度以兼顾 NAT 出口场景
     */
    int LOGIN_MAX_FAIL_BY_IP = 10;
    /**
     * 登录用户令牌的键的前缀
     */
    String PREFIX_USER_TOKEN = "PREFIX_USER_TOKEN_";
    /**
     * 验证码 每分钟最多验证次数
     */
    int CAPTCHA_MAX_VERIFY_TIMES_PER_MINUTE = 10;

    /**
     * 验证码 验证容差（像素）
     */
    int CAPTCHA_VERIFY_TOLERANCE = 5;

    /**
     * 按 用户名-IP 索引 token 的键前缀（用于同一IP重复登录检测）
     */
    String PREFIX_USER_BY_IP = "PREFIX_USER_BY_IP_";
    /**
     * 已通过滑块验证的验证码 token 标记前缀（登录时校验后立即删除，保证单次有效、防重放/防伪造）
     */
    String PREFIX_CAPTCHA_TOKEN = "PREFIX_CAPTCHA_TOKEN_";
    /**
     * 系统参数：验证码开关（sys_config.config_key）
     */
    String CONFIG_CAPTCHA_ENABLED = "captchaEnabled";
    /**
     * 自助注册次数限制前缀（按来源 IP，防批量注册刷库）
     */
    String PREFIX_REGISTER_TIMES_IP = "PREFIX_REGISTER_TIMES_IP_";
    /**
     * 找回密码邮箱验证码：Redis 键前缀（按用户名），值为 6 位验证码
     */
    String PREFIX_PWD_EMAIL_CODE = "PREFIX_PWD_EMAIL_CODE_";
    /**
     * 找回密码邮箱验证码错误次数：Redis 键前缀（按用户名）
     */
    String PREFIX_PWD_EMAIL_CODE_FAIL = "PREFIX_PWD_EMAIL_CODE_FAIL_";
    /**
     * 找回密码邮件重发间隔限制前缀（按用户名维度）
     */
    String PREFIX_PWD_EMAIL_INTERVAL_USER = "PREFIX_PWD_EMAIL_INTERVAL_U_";
    /**
     * 找回密码邮件重发间隔限制前缀（按来源 IP 维度，防邮件轰炸/扫号）
     */
    String PREFIX_PWD_EMAIL_INTERVAL_IP = "PREFIX_PWD_EMAIL_INTERVAL_IP_";
    /**
     * 找回密码邮件发送次数限制前缀（按来源 IP 维度，窗口计数）
     */
    String PREFIX_PWD_EMAIL_TIMES_IP = "PREFIX_PWD_EMAIL_TIMES_IP_";
    /**
     * 找回密码一次性重置令牌前缀：值为用户名，校验通过即删，防止验证码/链接重放
     */
    String PREFIX_PWD_RESET_TOKEN = "PREFIX_PWD_RESET_TOKEN_";
    /**
     * 邮箱验证码有效期（秒）：5 分钟
     */
    int PWD_EMAIL_CODE_TTL_SECONDS = 300;
    /**
     * 密码重置令牌有效期（秒）：验证码校验通过后 10 分钟内可完成重置
     */
    int PWD_RESET_TOKEN_TTL_SECONDS = 600;
    /**
     * 找回密码邮件重发最小间隔（秒）：同一账号/同一 IP 60 秒内仅可发送一次
     */
    int PWD_EMAIL_RESEND_INTERVAL_SECONDS = 60;
    /**
     * 找回密码邮件 IP 发送计数窗口（秒）与窗口内最大发送次数
     */
    int PWD_EMAIL_SEND_IP_WINDOW_SECONDS = 600;
    int PWD_EMAIL_SEND_IP_MAX = 5;
    /**
     * 邮箱验证码校验错误次数窗口（秒）与窗口内最大错误次数（防爆破验证码）
     */
    int PWD_EMAIL_FAIL_WINDOW_SECONDS = 600;
    int PWD_EMAIL_MAX_FAIL = 5;
    /**
     * 邮箱长度上限
     */
    int EMAIL_MAX_LENGTH = 50;
    /**
     * 注册/找回密码 IP 限流窗口（秒）：10 分钟
     */
    int SELF_SERVICE_WINDOW_SECONDS = 600;
    /**
     * 同一来源 IP 在窗口内允许的注册/找回密码最大次数
     */
    int SELF_SERVICE_MAX_BY_IP = 5;
    /**
     * 用户名长度范围
     */
    int USERNAME_MIN_LENGTH = 3;
    int USERNAME_MAX_LENGTH = 10;
    /**
     * 密码长度范围
     */
    int PASSWORD_MIN_LENGTH = 6;
    int PASSWORD_MAX_LENGTH = 20;
    /**
     * 邮箱格式正则（与注册/找回密码链路使用的规则保持一致），
     * 供用户中心更新邮箱等入口复用
     */
    String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    /**
     * 密码不合规时的统一提示
     */
    String PASSWORD_POLICY_TIP = "密码须为6-20位，且同时包含字母、数字和特殊字符！";
}
