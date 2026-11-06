package com.xuanchen.auth.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.entity.UserInfo;
import com.xuanchen.auth.auth.mapper.AuthMapper;
import com.xuanchen.auth.auth.service.IAuthService;
import com.xuanchen.auth.service.MailSendService;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.auth.utils.PasswordCodec;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.constant.CommonConst;
import com.xuanchen.common.server.WebSocketServer;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service接口实现类-->认证、授权
 *
 * @author XuanChen
 * @date 2025-03-13
 */
@Service
public class AuthServiceImpl extends ServiceImpl<AuthMapper, Auth> implements IAuthService {
    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private IAuthServiceCommon authServiceCommon;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private PasswordCodec passwordCodec;

    @Autowired
    private MailSendService mailSendService;

    /**
     * 邮箱验证码随机数生成器（SecureRandom 多线程共享安全）
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 邮箱格式校验：仅做实用级校验，真实可达性以"验证码能否收到"为准
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Override
    public Auth getByUserName(String userName) {
        QueryWrapper<Auth> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        return authMapper.selectOne(queryWrapper);
    }

    @Override
    public String getConfigValueByKey(String configKey) {
        return authMapper.getConfigValueByKey(configKey);
    }

    @Override
    public UserInfo getUserInfo(Auth sysUser, jakarta.servlet.http.HttpServletRequest request) {
        return getUserInfo(sysUser, request, null);
    }

    @Override
    public UserInfo getUserInfo(Auth sysUser, jakarta.servlet.http.HttpServletRequest request, String sessionId) {
        UserInfo userInfo = new UserInfo();
        if (sysUser == null) {
            return userInfo;
        }
        String userName = sysUser.getUserName();
        //使用 JOIN 查询加载人员信息字段
        java.util.Map<String, Object> raw = authMapper.getUserWithProfile(userName);
        //归一化 Map 键为 camelCase，防御不同 MyBatis 版本对 resultType=Map 的键映射差异
        java.util.Map<String, Object> userData = raw != null ? normalizeMapKeys(raw) : null;
        String token = JwtUtil.sign(userName);
        long ttlSeconds = JwtUtil.EXPIRE_TIME * 2 / 1000;
        String clientIp = com.xuanchen.common.utils.IPUtil.getClientIpAddress(request);
        String deviceType = com.xuanchen.common.utils.DeviceTypeUtil.detectDeviceType(request);
        // 会话ID：与会话/终端绑定，贯穿该会话的多次访问令牌轮换（续期时由调用方回传复用），
        // 作为"记住我"刷新令牌的绑定键，使强制退出能精确定位到单条会话
        String currentSessionId = StringUtil.isNotEmpty(sessionId)
                ? sessionId : UUID.randomUUID().toString().replace("-", "");
        String ipIndexKey = AuthConst.PREFIX_USER_BY_IP + userName + "_" + clientIp;
        Object oldToken = redisUtil.get(ipIndexKey);
        if (oldToken != null && !String.valueOf(oldToken).isEmpty()
                && !String.valueOf(oldToken).equals(token)) {
            // 被同IP新登录顶掉的旧会话：其刷新令牌必须一并失效，否则旧终端可凭刷新 Cookie 静默复活
            String replacedSessionId = authServiceCommon.getSessionIdByToken(String.valueOf(oldToken));
            if (StringUtil.isNotEmpty(replacedSessionId) && !replacedSessionId.equals(currentSessionId)) {
                authServiceCommon.revokeRefreshTokensBySession(userName, replacedSessionId);
            }
            redisUtil.del(AuthConst.PREFIX_USER_TOKEN + oldToken);
            redisUtil.del(AuthConst.PREFIX_ONLINE_META + oldToken);
        }
        redisUtil.set(AuthConst.PREFIX_USER_TOKEN + token, token, ttlSeconds);
        Map<String, Object> tokenMeta = new HashMap<>();
        tokenMeta.put("userName", userName);
        tokenMeta.put("token", token);
        tokenMeta.put("ip", clientIp);
        tokenMeta.put("deviceType", deviceType);
        tokenMeta.put("sessionId", currentSessionId);
        redisUtil.set(AuthConst.PREFIX_ONLINE_META + token, JsonUtils.toJson(tokenMeta), ttlSeconds);
        redisUtil.set(ipIndexKey, token, ttlSeconds);
        redisUtil.del(AuthConst.PREFIX_LOGIN_FAIL_TIMES + userName);
        userInfo.setId(sysUser.getId());
        userInfo.setUserName(userName);
        if (userData != null) {
            userInfo.setNickName((String) userData.get("nickName"));
            userInfo.setMobile((String) userData.get("mobile"));
            userInfo.setEmail((String) userData.get("email"));
            userInfo.setAvatar((String) userData.get("avatar"));
        }
        userInfo.setToken(token);
        userInfo.setPwdResetRequired(CommonConst.YES.equals(sysUser.getPwdResetRequired()) ? CommonConst.YES : CommonConst.NO);
        return userInfo;
    }

    @Override
    public void clearLogoutSession(String username, String token) {
        if (StringUtil.isNotEmpty(token)) {
            // 先取会话ID再删会话（删除后就查不到了）：WebSocket 长连接按会话精确断开。
            // 不这样做的话，退出登录后长连接依旧存活、仍能收到服务端推送
            String sessionId = authServiceCommon.getSessionIdByToken(token);
            redisUtil.del(AuthConst.PREFIX_USER_TOKEN + token);
            redisUtil.del(AuthConst.PREFIX_ONLINE_META + token);
            WebSocketServer.closeSessionOnLogout(sessionId, token);
        }
        Set<String> indexKeys = redisUtil.scanKeys(AuthConst.PREFIX_USER_BY_IP + username + "_*");
        if (indexKeys == null || indexKeys.isEmpty()) {
            return;
        }
        for (String indexKey : indexKeys) {
            Object indexedToken = redisUtil.get(indexKey);
            if (indexedToken == null) {
                redisUtil.del(indexKey);
                continue;
            }
            String indexed = indexedToken.toString();
            if (StringUtil.isNotEmpty(token) && token.equals(indexed)) {
                redisUtil.del(indexKey);
                continue;
            }
            if (redisUtil.get(AuthConst.PREFIX_USER_TOKEN + indexed) == null) {
                redisUtil.del(indexKey);
            }
        }
    }

    @Override
    public String validateCaptcha(Auth auth, boolean forceEnabled) {
        if (!forceEnabled) {
            String captchaEnabled = getConfigValueByKey(AuthConst.CONFIG_CAPTCHA_ENABLED);
            if ("false".equalsIgnoreCase(captchaEnabled)) {
                return null;
            }
        }
        String captchaToken = auth.getCaptchaToken();
        if (StringUtil.isEmpty(captchaToken)) {
            return "请完成验证码验证！";
        }
        boolean captchaValid;
        try {
            captchaValid = JwtUtil.verifyCaptchaToken(captchaToken, auth.getCaptchaId());
        } catch (Exception e) {
            captchaValid = false;
        }
        if (!captchaValid) {
            return "验证码验证失败，请重新验证！";
        }
        String captchaKey = AuthConst.PREFIX_CAPTCHA_TOKEN + captchaToken;
        if (redisUtil.get(captchaKey) == null) {
            return "验证码已失效或已被使用，请重新验证！";
        }
        redisUtil.del(captchaKey);
        return null;
    }

    @Override
    public String validateAccountFormat(String userName, String password) {
        if (StringUtil.isEmpty(userName)
                || userName.length() < AuthConst.USERNAME_MIN_LENGTH
                || userName.length() > AuthConst.USERNAME_MAX_LENGTH) {
            return "用户名长度须为" + AuthConst.USERNAME_MIN_LENGTH + "-" + AuthConst.USERNAME_MAX_LENGTH + "个字符！";
        }
        if (StringUtil.isEmpty(password)
                || password.length() < AuthConst.PASSWORD_MIN_LENGTH
                || password.length() > AuthConst.PASSWORD_MAX_LENGTH) {
            return "密码长度须为" + AuthConst.PASSWORD_MIN_LENGTH + "-" + AuthConst.PASSWORD_MAX_LENGTH + "个字符！";
        }
        return null;
    }

    @Override
    public String validateEmailFormat(String email) {
        if (StringUtil.isEmpty(email)) {
            return "请输入邮箱！";
        }
        if (email.length() > AuthConst.EMAIL_MAX_LENGTH) {
            return "邮箱长度不能超过" + AuthConst.EMAIL_MAX_LENGTH + "个字符！";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "邮箱格式不正确！";
        }
        return null;
    }

    @Override
    public boolean isSelfServiceOverTimes(String keyPrefix, String clientIp) {
        if (StringUtil.isEmpty(clientIp)) {
            return false;
        }
        return getLoginFailTimes(keyPrefix + clientIp) >= AuthConst.SELF_SERVICE_MAX_BY_IP;
    }

    @Override
    public void addSelfServiceTimes(String keyPrefix, String clientIp) {
        if (StringUtil.isNotEmpty(clientIp)) {
            redisUtil.incrWithExpire(keyPrefix + clientIp, AuthConst.SELF_SERVICE_WINDOW_SECONDS);
        }
    }

    @Override
    public boolean isEmailSendIpLimited(String clientIp) {
        if (StringUtil.isEmpty(clientIp)) {
            return false;
        }
        if (redisUtil.get(AuthConst.PREFIX_PWD_EMAIL_INTERVAL_IP + clientIp) != null) {
            return true;
        }
        return getLoginFailTimes(AuthConst.PREFIX_PWD_EMAIL_TIMES_IP + clientIp)
                >= AuthConst.PWD_EMAIL_SEND_IP_MAX;
    }

    @Override
    public String generateEmailCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    @Override
    public void kickOutAllSessions(String username) {
        // 重置密码属于"账号级失效"：撤销全部"记住我"刷新令牌 + 断开全部 WebSocket 长连接 +
        // 删除全部在线会话（含会话元数据）。与账号"停用/删除"共用同一套处置，避免只删访问令牌时
        // 客户端凭刷新 Cookie 静默复活（勾选"记住我"时必现）
        authServiceCommon.kickOutAccount(username, "密码已重置，请重新登录");
    }

    @Override
    public boolean isLoginSuccess(String username, String password, Auth sysUser, String clientIp) {
        if (sysUser == null) {
            addLoginFailOvertimes(username, clientIp);
            return false;
        }
        String storedHash = sysUser.getPassword();
        if (!passwordCodec.matches(password, storedHash, sysUser.getSalt())) {
            addLoginFailOvertimes(username, clientIp);
            return false;
        }
        if (!passwordCodec.isBcrypt(storedHash)) {
            String upgradedHash = passwordCodec.encode(password);
            UpdateWrapper<Auth> updateWrapper = new UpdateWrapper<>();
            updateWrapper.set("password", upgradedHash)
                    .set("salt", null)
                    .eq("user_name", username);
            update(updateWrapper);
            sysUser.setPassword(upgradedHash);
            sysUser.setSalt(null);
        }
        return true;
    }

    @Override
    public boolean isLoginFailOverTimes(String username, String clientIp) {
        boolean overByUser = StringUtil.isNotEmpty(username)
                && getLoginFailTimes(AuthConst.PREFIX_LOGIN_FAIL_TIMES + username)
                >= AuthConst.LOGIN_MAX_FAIL_BY_USERNAME;
        boolean overByIp = StringUtil.isNotEmpty(clientIp)
                && getLoginFailTimes(AuthConst.PREFIX_LOGIN_FAIL_TIMES_IP + clientIp)
                >= AuthConst.LOGIN_MAX_FAIL_BY_IP;
        return overByUser || overByIp;
    }

    @Override
    public int getLoginFailTimes(String key) {
        Object failTimes = redisUtil.get(key);
        if (failTimes == null) {
            return 0;
        }
        try {
            return Integer.parseInt(failTimes.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public String resetPasswordWithToken(String resetToken, String newPassword) {
        if (StringUtil.isEmpty(resetToken) || resetToken.length() > 64) {
            return "重置凭证无效，请重新获取验证码！";
        }
        if (StringUtil.isEmpty(newPassword)
                || newPassword.length() < AuthConst.PASSWORD_MIN_LENGTH
                || newPassword.length() > AuthConst.PASSWORD_MAX_LENGTH) {
            return "密码长度须为" + AuthConst.PASSWORD_MIN_LENGTH + "-" + AuthConst.PASSWORD_MAX_LENGTH + "个字符！";
        }
        String tokenKey = AuthConst.PREFIX_PWD_RESET_TOKEN + resetToken;
        Object usernameObj = redisUtil.get(tokenKey);
        if (usernameObj == null) {
            return "重置凭证已失效，请重新获取验证码！";
        }
        redisUtil.del(tokenKey);
        String userName = usernameObj.toString();
        Auth sysUser = getByUserName(userName);
        if (sysUser == null) {
            return "账号状态异常，请联系管理员！";
        }
        UpdateWrapper<Auth> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", sysUser.getId())
                .set("password", authServiceCommon.encryptPassword(newPassword))
                .set("salt", null)
                .set("pwd_reset_required", CommonConst.NO);
        update(updateWrapper);
        kickOutAllSessions(userName);
        return null;
    }

    @Override
    public String sendEmailCodeForPassword(String userName, String clientIp) {
        //使用 JOIN 查询加载邮箱字段（邮箱在人员信息表）
        java.util.Map<String, Object> raw = authMapper.getUserWithProfile(userName);
        java.util.Map<String, Object> userData = raw != null ? normalizeMapKeys(raw) : null;
        String email = userData != null ? (String) userData.get("email") : null;
        if (StringUtil.isNotEmpty(email)) {
            Object intervalFlag = redisUtil.get(AuthConst.PREFIX_PWD_EMAIL_INTERVAL_USER + userName);
            if (intervalFlag == null) {
                String code = generateEmailCode();
                redisUtil.set(AuthConst.PREFIX_PWD_EMAIL_CODE + userName, code,
                        AuthConst.PWD_EMAIL_CODE_TTL_SECONDS);
                redisUtil.del(AuthConst.PREFIX_PWD_EMAIL_CODE_FAIL + userName);
                try {
                    mailSendService.sendPasswordResetCode(email, code);
                } catch (IllegalStateException e) {
                    return e.getMessage();
                }
                redisUtil.set(AuthConst.PREFIX_PWD_EMAIL_INTERVAL_USER + userName, "1",
                        AuthConst.PWD_EMAIL_RESEND_INTERVAL_SECONDS);
            }
        }
        redisUtil.set(AuthConst.PREFIX_PWD_EMAIL_INTERVAL_IP + clientIp, "1",
                AuthConst.PWD_EMAIL_RESEND_INTERVAL_SECONDS);
        redisUtil.incrWithExpire(AuthConst.PREFIX_PWD_EMAIL_TIMES_IP + clientIp,
                AuthConst.PWD_EMAIL_SEND_IP_WINDOW_SECONDS);
        return null;
    }

    @Override
    public Map<String, String> verifyEmailCodeForResetToken(String userName, String emailCode) {
        int failTimes = getLoginFailTimes(AuthConst.PREFIX_PWD_EMAIL_CODE_FAIL + userName);
        if (failTimes >= AuthConst.PWD_EMAIL_MAX_FAIL) {
            return null;
        }
        Object storedCode = redisUtil.get(AuthConst.PREFIX_PWD_EMAIL_CODE + userName);
        if (storedCode == null || !emailCode.equals(storedCode.toString())) {
            redisUtil.incrWithExpire(AuthConst.PREFIX_PWD_EMAIL_CODE_FAIL + userName,
                    AuthConst.PWD_EMAIL_FAIL_WINDOW_SECONDS);
            return null;
        }
        redisUtil.del(AuthConst.PREFIX_PWD_EMAIL_CODE + userName);
        redisUtil.del(AuthConst.PREFIX_PWD_EMAIL_CODE_FAIL + userName);
        String resetToken = UUID.randomUUID().toString().replace("-", "");
        redisUtil.set(AuthConst.PREFIX_PWD_RESET_TOKEN + resetToken, userName,
                AuthConst.PWD_RESET_TOKEN_TTL_SECONDS);
        Map<String, String> data = new HashMap<>();
        data.put("resetToken", resetToken);
        return data;
    }

    @Override
    public String encryptPassword(String rawPassword) {
        return authServiceCommon.encryptPassword(rawPassword);
    }

    /**
     * 记录登录失败次数：用户名维度 + 来源 IP 维度
     */
    private void addLoginFailOvertimes(String username, String clientIp) {
        if (StringUtil.isNotEmpty(username)) {
            redisUtil.incrWithExpire(AuthConst.PREFIX_LOGIN_FAIL_TIMES + username,
                    AuthConst.LOGIN_FAIL_WINDOW_SECONDS);
        }
        if (StringUtil.isNotEmpty(clientIp)) {
            redisUtil.incrWithExpire(AuthConst.PREFIX_LOGIN_FAIL_TIMES_IP + clientIp,
                    AuthConst.LOGIN_FAIL_WINDOW_SECONDS);
        }
    }

    @Override
    public long countAccountByEmail(String email) {
        return authMapper.countAccountByEmail(email);
    }

    /**
     * 归一化 Map 键为 camelCase（防御不同 MyBatis 版本对 resultType=Map 的键映射差异）
     */
    private java.util.Map<String, Object> normalizeMapKeys(java.util.Map<String, Object> raw) {
        java.util.Map<String, Object> normalized = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, Object> entry : raw.entrySet()) {
            String key = entry.getKey();
            normalized.put(key, entry.getValue());
            if (key.contains("_")) {
                normalized.put(snakeToCamel(key), entry.getValue());
            }
        }
        return normalized;
    }

    private static String snakeToCamel(String snake) {
        StringBuilder sb = new StringBuilder(snake.length());
        boolean upper = false;
        for (int i = 0; i < snake.length(); i++) {
            char c = snake.charAt(i);
            if (c == '_') {
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return sb.toString();
    }
}
