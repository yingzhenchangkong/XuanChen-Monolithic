package com.xuanchen.auth.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.entity.UserInfo;
import com.xuanchen.auth.auth.service.IAuthService;
import com.xuanchen.auth.utils.CaptchaUtil;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.constant.CommonConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.ILogLoginServiceCommon;
import com.xuanchen.common.utils.DeviceTypeUtil;
import com.xuanchen.common.utils.IPUtil;
import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.StringUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

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
@RequiredArgsConstructor
public class AuthController {
    private final IAuthService authService;
    private final ILogLoginServiceCommon logLoginServiceCommon;
    private final RedisUtil redisUtil;

    /**
     * 登录
     * 规则：
     * - 同一用户名 + 同一IP 不允许重复登录（后登录者会踢掉前一次登录）
     * - 不同终端（手机端/电脑端）可同时登录
     * - 不同IP可同时登录
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public Result<UserInfo> login(@RequestBody Auth auth, HttpServletRequest request,
                                  HttpServletResponse response) {
        //1、验证码校验（开关由系统参数 captchaEnabled 控制，仅当显式为 false 时关闭，缺省按开启处理）
        String captchaError = authService.validateCaptcha(auth, false);
        if (captchaError != null) {
            return Result.badRequest(captchaError);
        }
        //2、校验登录失败次数（用户名 + 来源 IP 双维度，任一超限即拒绝）
        String username = auth.getUserName();
        String password = auth.getPassword();
        String clientIp = IPUtil.getClientIpAddress(request);
        if (authService.isLoginFailOverTimes(username, clientIp)) {
            logLoginServiceCommon.recordLoginLog(username, false, "登录失败次数过多", request);
            return Result.badRequest("登录失败次数过多，请稍后再试！");
        }
        Auth sysUser = authService.getByUserName(username);
        //3、校验用户名密码
        if (!authService.isLoginSuccess(password, sysUser)) {
            authService.addLoginFailOvertimes(username, clientIp);
            logLoginServiceCommon.recordLoginLog(username, false, "用户名或密码错误", request);
            return Result.badRequest("用户名或密码错误！");
        }
        //3.1、校验账号状态（status=1正常，2冻结），冻结账号不允许登录
        if (sysUser.getStatus() == null || sysUser.getStatus() != 1) {
            logLoginServiceCommon.recordLoginLog(username, false, "账号已冻结", request);
            return Result.badRequest("账号已被冻结，请联系管理员！");
        }
        //4. 获取终端类型，生成 token 并执行同IP踢下线逻辑（clientIp 已在上方取得）
        String deviceType = DeviceTypeUtil.detectDeviceType(request);
        // 会话ID在本次登录时生成并写入会话元数据，同时作为"记住我"刷新令牌的绑定键，
        // 使强制退出能按"会话/终端"精确定位（同一会话多次续期期间保持不变）
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        UserInfo userInfo = authService.getUserInfo(sysUser, clientIp, deviceType, sessionId);

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
            return Result.badRequest("退出登录失败！");
        }
        String username = JwtUtil.getUsername(token);
        if (StringUtil.isEmpty(username)) {
            return Result.badRequest("退出登录失败！");
        } else {
            Auth sysUser = authService.getByUserName(username);
            if (sysUser != null) {
                authService.clearLogoutSession(username, token);
                clearRefreshTokenFromRequest(request, response);
                return Result.success("退出登录成功！");
            } else {
                return Result.badRequest("退出登录失败！");
            }
        }
    }

    /**
     * 自助注册
     * 规则：
     * - 与登录相同的滑块验证码（开关跟随 captchaEnabled）+ IP 限流，防机器人批量注册
     * - 仅接收 userName/nickName/email/password 白名单字段，杜绝角色/状态等字段批量赋值提权
     * - 邮箱为必填（忘记密码时凭绑定邮箱接收验证码完成身份认证），存储在 sys_account，全系统账号内唯一
     * - 新账号 status=1（正常）、pwd_reset_required=0（密码由本人设置），不分配任何角色/部门/岗位，
     * 登录后仅可见无需权限的页面，由管理员另行授权
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public Result<String> register(@RequestBody Auth auth, HttpServletRequest request) {
        String userName = auth.getUserName() == null ? null : auth.getUserName().trim();
        String password = auth.getPassword();
        //1、格式校验（与前端校验规则保持一致，服务端为最终防线）
        String formatError = authService.validateAccountFormat(userName, password);
        if (formatError != null) {
            return Result.badRequest(formatError);
        }
        String nickName = auth.getNickName();
        if (StringUtil.isNotEmpty(nickName) && nickName.trim().length() > 20) {
            return Result.badRequest("昵称长度不能超过20个字符！");
        }
        //邮箱必填、格式合法（找回密码依赖绑定邮箱做身份认证）
        String email = auth.getEmail() == null ? null : auth.getEmail().trim().toLowerCase();
        String emailError = authService.validateEmailFormat(email);
        if (emailError != null) {
            return Result.badRequest(emailError);
        }
        String clientIp = IPUtil.getClientIpAddress(request);
        //2、IP 限流（滑块 token 仅 5 分钟单次有效，叠加窗口计数防批量注册）
        if (authService.isSelfServiceOverTimes(AuthConst.PREFIX_REGISTER_TIMES_IP, clientIp)) {
            return Result.badRequest("操作过于频繁，请稍后再试！");
        }
        //3、滑块验证码校验
        String captchaError = authService.validateCaptcha(auth, false);
        if (captchaError != null) {
            return Result.badRequest(captchaError);
        }
        //4、用户名查重（@TableLogic 自动排除已删除账号，删除后用户名可重新注册）
        if (authService.getByUserName(userName) != null) {
            return Result.badRequest("用户名已存在，请更换用户名！");
        }
        //5、邮箱查重：一个邮箱只能绑定一个正常账号（大小写不敏感），保证找回密码验证码有唯一归属
        if (authService.countAccountByEmail(email) > 0) {
            return Result.badRequest("该邮箱已被其他账号绑定，请更换邮箱！");
        }
        //6、组装账号：昵称/邮箱直接写入 sys_account（身份与凭证同源），注册不再创建空人员档案
        Auth newUser = new Auth();
        newUser.setUserName(userName);
        newUser.setNickName(StringUtil.isNotEmpty(nickName) ? nickName.trim() : userName);
        newUser.setEmail(email);
        newUser.setPassword(authService.encryptPassword(password));
        newUser.setStatus(CommonConst.STATUS_ENABLED);
        newUser.setPwdResetRequired(CommonConst.NO);
        newUser.setDelFlag(CommonConst.DEL_FLAG_NORMAL);
        authService.save(newUser);
        //限流计数在注册成功后写入
        authService.addSelfServiceTimes(AuthConst.PREFIX_REGISTER_TIMES_IP, clientIp);
        return Result.success("注册成功，请使用新账号登录！");
    }

    /**
     * 找回密码第 1 步：发送邮箱验证码
     * 安全规则：
     * - 滑块验证码强制校验（不受 captchaEnabled 开关影响，发邮件必须防机器人/防轰炸）
     * - IP 维度：60 秒重发间隔 + 10 分钟最多 5 封；用户名维度：60 秒重发间隔
     * - 账号不存在或未绑定邮箱时返回与成功完全一致的文案，不暴露账号是否存在（防用户名枚举）
     * - 验证码 6 位数字、Redis 保存 5 分钟，重发即覆盖旧码
     */
    @RequestMapping(value = "/password/sendEmailCode", method = RequestMethod.POST)
    public Result<String> sendPasswordEmailCode(@RequestBody Auth auth, HttpServletRequest request) {
        String userName = auth.getUserName() == null ? null : auth.getUserName().trim();
        if (StringUtil.isEmpty(userName)
                || userName.length() < AuthConst.USERNAME_MIN_LENGTH
                || userName.length() > AuthConst.USERNAME_MAX_LENGTH) {
            return Result.badRequest("用户名长度须为" + AuthConst.USERNAME_MIN_LENGTH + "-" + AuthConst.USERNAME_MAX_LENGTH + "个字符！");
        }
        String clientIp = IPUtil.getClientIpAddress(request);
        //1、IP 发信频控：重发间隔 + 窗口总量（任何请求都计数，防止不存在的用户名被用来刷邮件接口）
        if (authService.isEmailSendIpLimited(clientIp)) {
            return Result.badRequest("验证码发送过于频繁，请稍后再试！");
        }
        //2、滑块验证码强制校验（发信场景即使系统关闭了登录验证码也必须验证）
        String captchaError = authService.validateCaptcha(auth, true);
        if (captchaError != null) {
            return Result.badRequest(captchaError);
        }
        //3、查询账号及其绑定邮箱；不存在/未绑定邮箱一律走统一成功文案（不发信），防枚举
        authService.sendEmailCodeForPassword(userName, clientIp);
        return Result.success("验证码已发送，若账号存在且已绑定邮箱，请注意查收（5 分钟内有效）！");
    }

    /**
     * 找回密码第 2 步：校验邮箱验证码，换发一次性短期重置令牌
     * - 错误验证码按用户名维度计数，10 分钟内错 5 次锁定（防 6 位数字码爆破）
     * - 校验通过立即删除验证码（不可再用），另发 10 分钟有效的重置令牌承接第 3 步
     * - 账号/验证码不存在均返回相同错误，不泄露账号是否存在
     */
    @RequestMapping(value = "/password/verifyEmailCode", method = RequestMethod.POST)
    public Result<Map<String, String>> verifyPasswordEmailCode(@RequestBody Auth auth) {
        String userName = auth.getUserName() == null ? null : auth.getUserName().trim();
        String emailCode = auth.getEmailCode();
        if (StringUtil.isEmpty(userName)
                || userName.length() < AuthConst.USERNAME_MIN_LENGTH
                || userName.length() > AuthConst.USERNAME_MAX_LENGTH) {
            return Result.badRequest("用户名长度须为" + AuthConst.USERNAME_MIN_LENGTH + "-" + AuthConst.USERNAME_MAX_LENGTH + "个字符！");
        }
        if (StringUtil.isEmpty(emailCode) || !emailCode.matches("^\\d{6}$")) {
            return Result.badRequest("请输入6位数字验证码！");
        }
        Map<String, String> data = authService.verifyEmailCodeForResetToken(userName, emailCode);
        if (data == null) {
            int failTimes = authService.getLoginFailTimes(AuthConst.PREFIX_PWD_EMAIL_CODE_FAIL + userName);
            if (failTimes >= AuthConst.PWD_EMAIL_MAX_FAIL) {
                return Result.badRequest("验证码错误次数过多，请重新获取验证码！");
            }
            return Result.badRequest("验证码错误或已过期！");
        }
        return Result.success("验证成功，请设置新密码！", data);
    }

    /**
     * 找回密码第 3 步：凭一次性重置令牌设置新密码
     * - 令牌不存在/已使用/已过期一律拒绝；读取后立即删除（单次有效，防并发/重放）
     * - 重置成功后踢出该账号全部在线会话，防止旧令牌在改密后继续使用
     */
    @RequestMapping(value = "/password/reset", method = RequestMethod.POST)
    public Result<String> resetPassword(@RequestBody Auth auth) {
        String error = authService.resetPasswordWithToken(auth.getResetToken(), auth.getPassword());
        if (error != null) {
            return Result.badRequest(error);
        }
        return Result.success("密码已重置，请使用新密码登录！");
    }

    /**
     * 凭刷新令牌签发新访问令牌（公开接口，httpOnly Cookie 自动携带）。
     * 访问令牌过期后前端静默调用此接口，成功则拿到新访问令牌继续操作，
     * 失败（刷新令牌过期/被篡改/账号已冻结）则前端跳转登录页重新输入密码。
     */
    @RequestMapping(value = "/auth/refresh", method = RequestMethod.POST)
    public Result<UserInfo> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        if (StringUtil.isEmpty(refreshToken)) {
            return Result.badRequest("登录已过期，请重新登录！");
        }
        Object refreshValue = redisUtil.get(AuthConst.PREFIX_REFRESH_TOKEN + refreshToken);
        if (refreshValue == null) {
            // 刷新令牌已过期/已失效（Redis 无记录）：清掉浏览器上的陈旧 Cookie，
            // 否则每次冷启动前端都会白跑一次刷新请求，且用户永远留着无效凭据
            clearRefreshCookie(response);
            return Result.badRequest("登录已过期，请重新登录！");
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
                return Result.badRequest("登录已过期，请重新登录！");
            }
            username = payload.get("username") == null ? null : payload.get("username").toString();
            sessionId = payload.get("sessionId") == null ? null : payload.get("sessionId").toString();
        } else {
            username = rawValue;
        }
        if (StringUtil.isEmpty(username)) {
            clearRefreshCookie(response);
            return Result.badRequest("登录已过期，请重新登录！");
        }
        Auth sysUser = authService.getByUserName(username);
        if (sysUser == null || sysUser.getStatus() == null || sysUser.getStatus() != 1) {
            redisUtil.del(AuthConst.PREFIX_REFRESH_TOKEN + refreshToken);
            clearRefreshCookie(response);
            return Result.badRequest("账号状态异常，请重新登录！");
        }
        String clientIp = IPUtil.getClientIpAddress(request);
        String deviceType = DeviceTypeUtil.detectDeviceType(request);
        // 复用刷新令牌中绑定的会话ID：同一会话在多次续期（访问令牌轮换）间身份保持稳定，
        // 这样管理员对其中任一次签发的访问令牌"强制退出"，都能按会话ID撤销本枚刷新令牌
        UserInfo userInfo = authService.getUserInfo(sysUser, clientIp, deviceType, sessionId);
        return Result.success("刷新成功", userInfo);
    }

    /**
     * 签发刷新令牌：生成 UUID 写入 Redis（值为 JSON{"username":用户名,"sessionId":会话ID}，
     * 7 天 TTL；sessionId 把该刷新令牌绑定到具体会话/终端），同时设置 httpOnly Cookie。
     * Cookie 路径为 "/"，SameSite=Lax（防 CSRF），secure 标志跟随请求协议（HTTPS 环境自动开启）。
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
     * 清除刷新令牌 Cookie：maxAge=0 通知浏览器立即删除该 Cookie
     */
    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(AuthConst.REFRESH_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * 从请求中读取刷新令牌 Cookie 值，不存在返回 null
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
     * 退出登录时清除刷新令牌：从 Cookie 读取令牌值 → 删除 Redis 键 → 置 Cookie 过期
     */
    private void clearRefreshTokenFromRequest(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        if (StringUtil.isNotEmpty(refreshToken)) {
            redisUtil.del(AuthConst.PREFIX_REFRESH_TOKEN + refreshToken);
        }
        clearRefreshCookie(response);
    }

    /**
     * 生成验证码
     */
    @GetMapping("/captcha/generate")
    public Result<Map<String, Object>> captchaGenerate() {
        String captchaId = UUID.randomUUID().toString();

        // 读取验证码背景图类型配置（dynamic=动态生成，static=预置图片）
        String captchaType = authService.getConfigValueByKey(AuthConst.CONFIG_CAPTCHA_TYPE);
        Map<String, Object> captchaResult = CaptchaUtil.generateCaptcha(captchaType);

        Map<String, Object> map = new HashMap<>();
        map.put("captchaId", captchaId);
        map.put("bgImage", captchaResult.get("bgImage"));
        map.put("sliderImage", captchaResult.get("sliderImage"));
        map.put("sliderTop", captchaResult.get("offsetY"));
        map.put("imageWidth", captchaResult.get("imageWidth"));
        map.put("imageHeight", captchaResult.get("imageHeight"));
        map.put("sliderWidth", captchaResult.get("sliderWidth"));
        map.put("sliderHeight", captchaResult.get("sliderHeight"));

        // 只存储正确位置，不返回给前端
        redisUtil.set("captcha:" + captchaId, String.valueOf(captchaResult.get("offsetX")), 300);

        return Result.success(map);
    }

    /**
     * 校验验证码
     */
    @PostMapping("/captcha/verify")
    public Result<Map<String, Object>> captchaVerify(HttpServletRequest request, @RequestBody JsonNode jsonObject) {
        //1、获取客户端 IP
        String clientIp = IPUtil.getClientIpAddress(request);
        //2、检查防暴力破解限制：Lua 原子"自增+首次设TTL"（固定窗口），
        //   杜绝 get-then-set 并发覆盖计数、每次请求续期导致窗口顺延的问题
        String limitKey = "captcha:limit:" + clientIp;
        long count = redisUtil.incrWithExpire(limitKey, 60);
        if (count > AuthConst.CAPTCHA_MAX_VERIFY_TIMES_PER_MINUTE) {
            return Result.error("验证次数过多，请稍后再试！");
        }
        //4、获取参数
        String captchaId = jsonObject.get("captchaId").asText();
        Double captchaOffset = jsonObject.get("captchaOffset").asDouble();
        //5、检查captchaId是否存在且未过期
        Object storedCaptchaInfo = redisUtil.get("captcha:" + captchaId);
        if (storedCaptchaInfo == null) {
            return Result.error("验证失败，请重新验证！");
        }
        //6、检查偏移量是否正确
        double storedCaptchaOffset = Double.parseDouble(storedCaptchaInfo.toString());
        if (!CaptchaUtil.verifyOffset(captchaOffset, storedCaptchaOffset, AuthConst.CAPTCHA_VERIFY_TOLERANCE)) {
            return Result.error("验证失败，请重新验证！");
        }
        //7、生成后端验证token（5分钟有效期，密钥来自服务端配置）。失败时 JwtUtil 直接抛异常，
        //   不再返回 null（避免出现"成功但 token 为 null"的矛盾响应），由全局异常处理器统一返回 500
        String captchaToken = JwtUtil.createCaptchaToken(captchaId, JwtUtil.CAPTCHA_EXPIRE_SECONDS);
        //8、删除已使用的滑块答案，并写入"已通过验证"标记；登录时凭该标记单次放行，防止 token 重放/自签绕过
        redisUtil.del("captcha:" + captchaId);
        redisUtil.set(AuthConst.PREFIX_CAPTCHA_TOKEN + captchaToken, "1", JwtUtil.CAPTCHA_EXPIRE_SECONDS);
        //9、返回验证token
        Map<String, Object> map = new HashMap<>();
        map.put("captchaToken", captchaToken);
        return Result.success(map);
    }
}
