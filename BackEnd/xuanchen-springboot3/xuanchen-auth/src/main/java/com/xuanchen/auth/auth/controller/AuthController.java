package com.xuanchen.auth.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.entity.UserInfo;
import com.xuanchen.auth.auth.service.IAuthService;
import com.xuanchen.auth.utils.CaptchaUtil;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.constant.CommonConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.ILogLoginServiceCommon;
import com.xuanchen.common.utils.IPUtil;
import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.StringUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 控制器-->认证、授权
 *
 * @author XuanChen
 * @date 2025-03-13
 */
@RestController
public class AuthController {
    @Autowired
    private IAuthService authService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ILogLoginServiceCommon logLoginServiceCommon;

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public Result<UserInfo> login(@RequestBody Auth auth, HttpServletRequest request,
                                  HttpServletResponse response) {
        String username = auth.getUserName();
        String password = auth.getPassword();
        String clientIp = IPUtil.getClientIpAddress(request);
        //1、验证码校验（开关由系统参数 captchaEnabled 控制，仅当显式为 false 时关闭，缺省按开启处理）
        String captchaError = authService.validateCaptcha(auth, false);
        if (captchaError != null) {
            return Result.error(captchaError);
        }
        Auth sysUser = authService.getByUserName(username);
        //2、校验登录失败次数（用户名 + 来源 IP 双维度，任一超限即拒绝）
        if (authService.isLoginFailOverTimes(username, clientIp)) {
            logLoginServiceCommon.recordLoginLog(username, false, "登录失败次数过多", request);
            return Result.error("登录失败次数过多，请稍后再试！");
        }
        //2.1、校验账号状态：冻结账号禁止登录（在密码校验之前拦截，且不累计登录失败次数）
        if (sysUser != null && (sysUser.getStatus() == null || sysUser.getStatus() != 1)) {
            logLoginServiceCommon.recordLoginLog(username, false, "账号已冻结", request);
            return Result.error("账号已被冻结，请联系管理员！");
        }
        //3、校验用户是否存在，校验用户名密码是否正确
        if (!authService.isLoginSuccess(username, password, sysUser, clientIp)) {
            logLoginServiceCommon.recordLoginLog(username, false, "用户名或密码错误", request);
            return Result.error("用户名或密码错误！");
        }
        //4、返回用户信息
        // 会话ID在本次登录时生成并写入会话元数据，同时作为"记住我"刷新令牌的绑定键，
        // 使强制退出能按"会话/终端"精确定位（同一会话多次续期期间保持不变）
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        UserInfo userInfo = authService.getUserInfo(sysUser, request, sessionId);

        //5. 刷新令牌：先清除浏览器上可能残留的旧刷新凭据（Cookie + Redis），
        //   再按"记住我"决定是否签发新的 7 天刷新令牌（绑定本次会话ID）
        clearRefreshTokenFromRequest(request, response);
        if (Boolean.TRUE.equals(auth.getRememberMe())) {
            issueRefreshToken(username, sessionId, request, response);
        }

        logLoginServiceCommon.recordLoginLog(username, true, "登录成功", request);
        return Result.success("登录成功，欢迎回来！", userInfo);
    }

    /**
     * 退出登录
     */
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public Result<String> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = request.getHeader(AuthConst.XC_ACCESS_TOKEN);
        if (StringUtil.isEmpty(token)) {
            return Result.error("退出登录失败！");
        }
        String username = JwtUtil.getUsername(token);
        if (StringUtil.isEmpty(username)) {
            return Result.error("退出登录失败！");
        } else {
            Auth sysUser = authService.getByUserName(username);
            if (sysUser != null) {
                authService.clearLogoutSession(username, token);
                clearRefreshTokenFromRequest(request, response);
                SecurityUtils.getSubject().logout();
                return Result.success("退出登录成功！");
            } else {
                return Result.error("退出登录失败！");
            }
        }
    }

    /**
     * 自助注册（公开接口）
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public Result<String> register(@RequestBody Auth auth, HttpServletRequest request) {
        String userName = auth.getUserName() == null ? null : auth.getUserName().trim();
        String password = auth.getPassword();
        //1、格式校验
        String formatError = authService.validateAccountFormat(userName, password);
        if (formatError != null) {
            return Result.error(formatError);
        }
        String nickName = auth.getNickName();
        if (StringUtil.isNotEmpty(nickName) && nickName.trim().length() > 20) {
            return Result.error("昵称长度不能超过20个字符！");
        }
        //邮箱必填、格式合法
        String email = auth.getEmail() == null ? null : auth.getEmail().trim().toLowerCase();
        String emailError = authService.validateEmailFormat(email);
        if (emailError != null) {
            return Result.error(emailError);
        }
        String clientIp = IPUtil.getClientIpAddress(request);
        //2、IP 限流
        if (authService.isSelfServiceOverTimes(AuthConst.PREFIX_REGISTER_TIMES_IP, clientIp)) {
            return Result.error("操作过于频繁，请稍后再试！");
        }
        //3、滑块验证码校验
        String captchaError = authService.validateCaptcha(auth, false);
        if (captchaError != null) {
            return Result.error(captchaError);
        }
        //4、用户名查重
        if (authService.getByUserName(userName) != null) {
            return Result.error("用户名已存在，请更换用户名！");
        }
        //5、邮箱查重（账号表内唯一）
        if (authService.countAccountByEmail(email) > 0) {
            return Result.error("该邮箱已被其他账号绑定，请更换邮箱！");
        }
        //6、组装账号：昵称/邮箱直接写入 sys_account，注册不再创建空人员档案
        Auth newUser = new Auth();
        newUser.setUserName(userName);
        newUser.setNickName(StringUtil.isNotEmpty(nickName) ? nickName.trim() : userName);
        newUser.setEmail(email);
        newUser.setPassword(authService.encryptPassword(password));
        newUser.setStatus(CommonConst.STATUS_ENABLED);
        newUser.setPwdResetRequired(CommonConst.NO);
        newUser.setDelFlag(CommonConst.DEL_FLAG_NORMAL);
        authService.save(newUser);
        authService.addSelfServiceTimes(AuthConst.PREFIX_REGISTER_TIMES_IP, clientIp);
        return Result.success("注册成功，请使用新账号登录！");
    }

    /**
     * 找回密码第 1 步：发送邮箱验证码（公开接口）
     */
    @RequestMapping(value = "/password/sendEmailCode", method = RequestMethod.POST)
    public Result<String> sendPasswordEmailCode(@RequestBody Auth auth, HttpServletRequest request) {
        String userName = auth.getUserName() == null ? null : auth.getUserName().trim();
        if (StringUtil.isEmpty(userName)
                || userName.length() < AuthConst.USERNAME_MIN_LENGTH
                || userName.length() > AuthConst.USERNAME_MAX_LENGTH) {
            return Result.error("用户名长度须为" + AuthConst.USERNAME_MIN_LENGTH + "-" + AuthConst.USERNAME_MAX_LENGTH + "个字符！");
        }
        String clientIp = IPUtil.getClientIpAddress(request);
        //1、IP 发信频控
        if (authService.isEmailSendIpLimited(clientIp)) {
            return Result.error("验证码发送过于频繁，请稍后再试！");
        }
        //2、滑块验证码强制校验
        String captchaError = authService.validateCaptcha(auth, true);
        if (captchaError != null) {
            return Result.error(captchaError);
        }
        //3、发送验证码
        String sendError = authService.sendEmailCodeForPassword(userName, clientIp);
        if (sendError != null) {
            return Result.error(sendError);
        }
        return Result.success("验证码已发送，若账号存在且已绑定邮箱，请注意查收（5 分钟内有效）！");
    }

    /**
     * 找回密码第 2 步：校验邮箱验证码，换发一次性短期重置令牌（公开接口）
     */
    @RequestMapping(value = "/password/verifyEmailCode", method = RequestMethod.POST)
    public Result<Map<String, String>> verifyPasswordEmailCode(@RequestBody Auth auth) {
        String userName = auth.getUserName() == null ? null : auth.getUserName().trim();
        String emailCode = auth.getEmailCode();
        if (StringUtil.isEmpty(userName)
                || userName.length() < AuthConst.USERNAME_MIN_LENGTH
                || userName.length() > AuthConst.USERNAME_MAX_LENGTH) {
            return Result.error("用户名长度须为" + AuthConst.USERNAME_MIN_LENGTH + "-" + AuthConst.USERNAME_MAX_LENGTH + "个字符！");
        }
        if (StringUtil.isEmpty(emailCode) || !emailCode.matches("^\\d{6}$")) {
            return Result.error("请输入6位数字验证码！");
        }
        Map<String, String> data = authService.verifyEmailCodeForResetToken(userName, emailCode);
        if (data == null) {
            int failTimes = authService.getLoginFailTimes(AuthConst.PREFIX_PWD_EMAIL_CODE_FAIL + userName);
            if (failTimes >= AuthConst.PWD_EMAIL_MAX_FAIL) {
                return Result.error("验证码错误次数过多，请重新获取验证码！");
            }
            return Result.error("验证码错误或已过期！");
        }
        return Result.success("验证成功，请设置新密码！", data);
    }

    /**
     * 找回密码第 3 步：凭一次性重置令牌设置新密码（公开接口）
     */
    @RequestMapping(value = "/password/reset", method = RequestMethod.POST)
    public Result<String> resetPassword(@RequestBody Auth auth) {
        String error = authService.resetPasswordWithToken(auth.getResetToken(), auth.getPassword());
        if (error != null) {
            return Result.error(error);
        }
        return Result.success("密码已重置，请使用新密码登录！");
    }

    /**
     * 凭刷新令牌签发新访问令牌（公开接口，httpOnly Cookie 自动携带）
     */
    @RequestMapping(value = "/auth/refresh", method = RequestMethod.POST)
    public Result<UserInfo> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        if (StringUtil.isEmpty(refreshToken)) {
            return Result.error("登录已过期，请重新登录！");
        }
        Object refreshValue = redisUtil.get(AuthConst.PREFIX_REFRESH_TOKEN + refreshToken);
        if (refreshValue == null) {
            // 刷新令牌已过期/已失效（Redis 无记录）：清掉浏览器上的陈旧 Cookie，
            // 否则每次冷启动前端都会白跑一次刷新请求，且用户永远留着无效凭据
            clearRefreshCookie(response);
            return Result.error("登录已过期，请重新登录！");
        }
        // 刷新令牌值：新结构为 JSON {"username":..,"sessionId":..}，历史结构为纯用户名（兼容）
        String rawValue = refreshValue.toString();
        String username;
        String sessionId = null;
        if (rawValue.startsWith("{")) {
            Map<String, Object> payload;
            try {
                payload = JsonUtils.parseToMap(rawValue);
            } catch (Exception e) {
                clearRefreshCookie(response);
                return Result.error("登录已过期，请重新登录！");
            }
            username = payload.get("username") == null ? null : payload.get("username").toString();
            sessionId = payload.get("sessionId") == null ? null : payload.get("sessionId").toString();
        } else {
            username = rawValue;
        }
        if (StringUtil.isEmpty(username)) {
            clearRefreshCookie(response);
            return Result.error("登录已过期，请重新登录！");
        }
        Auth sysUser = authService.getByUserName(username);
        if (sysUser == null || sysUser.getStatus() == null || sysUser.getStatus() != 1) {
            redisUtil.del(AuthConst.PREFIX_REFRESH_TOKEN + refreshToken);
            clearRefreshCookie(response);
            return Result.error("账号状态异常，请重新登录！");
        }
        // 复用刷新令牌中绑定的会话ID：同一会话在多次续期（访问令牌轮换）间身份保持稳定，
        // 这样管理员对其中任一次签发的访问令牌"强制退出"，都能按会话ID撤销本枚刷新令牌
        UserInfo userInfo = authService.getUserInfo(sysUser, request, sessionId);
        return Result.success("刷新成功", userInfo);
    }

    /**
     * 生成验证码
     */
    @GetMapping("/captcha/generate")
    public Result<Map<String, Object>> captchaGenerate() {
        String captchaId = UUID.randomUUID().toString();
        String captchaType = authService.getConfigValueByKey(AuthConst.CONFIG_CAPTCHA_TYPE);
        Map<String, Object> captchaResult = CaptchaUtil.generateCaptcha(captchaType);
        Map<String, Object> map = new HashMap<>();
        map.put("captchaId", captchaId);
        map.put("bgImage", captchaResult.get("bgImage"));
        map.put("sliderImage", captchaResult.get("sliderImage"));
        map.put("sliderTop", captchaResult.get("offsetY"));
        redisUtil.set("captcha:" + captchaId, String.valueOf(captchaResult.get("offsetX")), 300);
        return Result.success(map);
    }

    /**
     * 校验验证码
     */
    @PostMapping("/captcha/verify")
    public Result<Map<String, Object>> captchaVerify(HttpServletRequest request, @RequestBody JsonNode jsonObject) {
        String clientIp = IPUtil.getClientIpAddress(request);
        String limitKey = "captcha:limit:" + clientIp;
        long count = redisUtil.incrWithExpire(limitKey, 60);
        if (count > AuthConst.CAPTCHA_MAX_VERIFY_TIMES_PER_MINUTE) {
            return Result.error("验证次数过多，请稍后再试！");
        }
        String captchaId = jsonObject.get("captchaId").asText();
        Double captchaOffset = jsonObject.get("captchaOffset").asDouble();
        Object storedCaptchaInfo = redisUtil.get("captcha:" + captchaId);
        if (storedCaptchaInfo == null) {
            return Result.error("验证失败，请重新验证！");
        }
        double storedCaptchaOffset = Double.parseDouble(storedCaptchaInfo.toString());
        if (captchaOffset == null
                || !CaptchaUtil.verifyOffset(captchaOffset, storedCaptchaOffset, AuthConst.CAPTCHA_VERIFY_TOLERANCE)) {
            return Result.error("验证失败，请重新验证！");
        }
        String captchaToken = JwtUtil.createCaptchaToken(captchaId, JwtUtil.CAPTCHA_EXPIRE_SECONDS);
        redisUtil.del("captcha:" + captchaId);
        redisUtil.set(AuthConst.PREFIX_CAPTCHA_TOKEN + captchaToken, "1", JwtUtil.CAPTCHA_EXPIRE_SECONDS);
        Map<String, Object> map = new HashMap<>();
        map.put("captchaToken", captchaToken);
        return Result.success(map);
    }

    /**
     * 签发刷新令牌：生成 UUID 写入 Redis（值为 JSON{"username":用户名,"sessionId":会话ID}，
     * 7 天 TTL；sessionId 把该刷新令牌绑定到具体会话/终端），同时设置 httpOnly Cookie
     */
    private void issueRefreshToken(String username, String sessionId, HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("sessionId", sessionId);
        redisUtil.set(AuthConst.PREFIX_REFRESH_TOKEN + refreshToken, JsonUtils.toJson(payload),
                AuthConst.REFRESH_TOKEN_EXPIRE_SECONDS);
        Cookie cookie = new Cookie(AuthConst.REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(AuthConst.REFRESH_TOKEN_EXPIRE_SECONDS);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
    }

    /**
     * 清除刷新令牌 Cookie
     */
    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(AuthConst.REFRESH_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * 从请求中读取刷新令牌 Cookie 值
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AuthConst.REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 退出登录时清除刷新令牌
     */
    private void clearRefreshTokenFromRequest(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        if (StringUtil.isNotEmpty(refreshToken)) {
            redisUtil.del(AuthConst.PREFIX_REFRESH_TOKEN + refreshToken);
        }
        clearRefreshCookie(response);
    }
}
