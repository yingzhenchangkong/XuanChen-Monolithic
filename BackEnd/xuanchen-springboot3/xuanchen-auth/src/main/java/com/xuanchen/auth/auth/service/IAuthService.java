package com.xuanchen.auth.auth.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.entity.UserInfo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * Service接口-->认证、授权
 *
 * @author XuanChen
 * @date 2025-03-13
 */
public interface IAuthService extends IService<Auth> {
    /**
     * 根据用户名获取用户信息
     *
     * @param userName 用户名
     * @return 用户信息
     */
    Auth getByUserName(String userName);

    /**
     * 按 config_key 查询启用中的系统参数值
     *
     * @param configKey 参数键
     * @return 参数值，不存在返回 null
     */
    String getConfigValueByKey(String configKey);

    /**
     * 获取用户信息（返回给前端），含 JWT 签发、会话写入、同 IP 踢旧等逻辑
     *
     * @param sysUser 用户实体
     * @param request 登录请求（用于获取登录IP、终端类型）
     * @return 用户信息
     */
    UserInfo getUserInfo(Auth sysUser, HttpServletRequest request);

    /**
     * 获取用户信息（返回给前端），并指定所签发会话的会话ID。
     * <p>
     * 登录时传 null（内部生成新会话ID，调用方自行写入"记住我"刷新令牌）；
     * 刷新令牌续期时传入刷新令牌中携带的会话ID，使同一会话在访问令牌轮换间身份稳定，
     * 从而"强制退出"能按会话精确定位并撤销该会话的刷新令牌。
     *
     * @param sessionId 会话ID；为空时生成新的
     */
    UserInfo getUserInfo(Auth sysUser, HttpServletRequest request, String sessionId);

    /**
     * 退出登录的会话清理（幂等）
     *
     * @param username 用户名
     * @param token    本次请求的token
     */
    void clearLogoutSession(String username, String token);

    /**
     * 校验滑块验证码
     *
     * @param auth         认证实体（包含captchaToken和captchaId）
     * @param forceEnabled true=强制校验（发邮件等敏感操作）；false=仅当系统参数未显式设为"false"时校验
     * @return 校验失败返回错误提示；通过返回null
     */
    String validateCaptcha(Auth auth, boolean forceEnabled);

    /**
     * 用户名/密码格式校验
     *
     * @param userName 用户名
     * @param password 密码
     * @return 校验失败返回错误提示；通过返回null
     */
    String validateAccountFormat(String userName, String password);

    /**
     * 邮箱格式校验
     *
     * @param email 邮箱
     * @return 校验失败返回错误提示；通过返回null
     */
    String validateEmailFormat(String email);

    /**
     * 注册/找回密码IP维度限流检查
     *
     * @param keyPrefix 键前缀
     * @param clientIp  客户端IP
     * @return 是否超限
     */
    boolean isSelfServiceOverTimes(String keyPrefix, String clientIp);

    /**
     * 写入注册/找回密码IP计数
     *
     * @param keyPrefix 键前缀
     * @param clientIp  客户端IP
     */
    void addSelfServiceTimes(String keyPrefix, String clientIp);

    /**
     * 找回密码发信IP频控检查
     *
     * @param clientIp 客户端IP
     * @return 是否被限流
     */
    boolean isEmailSendIpLimited(String clientIp);

    /**
     * 生成6位数字邮箱验证码
     *
     * @return 验证码
     */
    String generateEmailCode();

    /**
     * 踢出指定用户的全部在线会话
     *
     * @param username 用户名
     */
    void kickOutAllSessions(String username);

    /**
     * 校验是否登录成功（含双栈口令校验 + 历史哈希无感升级 + 失败计数）
     *
     * @param username 用户名
     * @param password 明文密码
     * @param sysUser  用户实体
     * @param clientIp 来源 IP（失败计数双维度用）
     * @return 是否登录成功
     */
    boolean isLoginSuccess(String username, String password, Auth sysUser, String clientIp);

    /**
     * 校验登录失败是否超限
     *
     * @param username 用户名
     * @param clientIp 客户端IP
     * @return 是否超限
     */
    boolean isLoginFailOverTimes(String username, String clientIp);

    /**
     * 读取失败计数
     *
     * @param key Redis键
     * @return 失败次数
     */
    int getLoginFailTimes(String key);

    /**
     * 凭一次性重置令牌设置新密码
     *
     * @param resetToken  重置令牌
     * @param newPassword 新密码
     * @return 错误信息，成功返回null
     */
    String resetPasswordWithToken(String resetToken, String newPassword);

    /**
     * 发送邮箱验证码
     *
     * @param userName 用户名
     * @param clientIp 客户端IP
     * @return 错误信息，成功返回null
     */
    String sendEmailCodeForPassword(String userName, String clientIp);

    /**
     * 校验邮箱验证码，换发一次性短期重置令牌
     *
     * @param userName  用户名
     * @param emailCode 邮箱验证码
     * @return 包含resetToken的Map，失败返回null
     */
    Map<String, String> verifyEmailCodeForResetToken(String userName, String emailCode);

    /**
     * 加密密码
     *
     * @param rawPassword 明文密码
     * @return 加密后的密码
     */
    String encryptPassword(String rawPassword);

    /**
     * 检查邮箱是否已被其他账号绑定（sys_account.email）
     *
     * @param email 邮箱
     * @return 匹配条数
     */
    long countAccountByEmail(String email);
}
