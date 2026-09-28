package com.xuanchen.auth.auth.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.entity.UserInfo;
import com.xuanchen.auth.auth.service.IAuthService;
import com.xuanchen.auth.service.MailSendService;
import com.xuanchen.auth.utils.CaptchaUtil;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.constant.CommonConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.DeviceTypeUtil;
import com.xuanchen.common.utils.IPUtil;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

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
    private final IAuthServiceCommon authServiceCommon;
    private final RedisUtil redisUtil;
    private final PasswordEncoder passwordEncoder;
    private final MailSendService mailSendService;

    /**
     * 邮箱验证码随机数生成器（线程内安全：SecureRandom 多线程共享安全）
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 邮箱格式校验：仅做实用级校验，真实可达性以"验证码能否收到"为准
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 登录
     * 规则：
     * - 同一用户名 + 同一IP 不允许重复登录（后登录者会踢掉前一次登录）
     * - 不同终端（手机端/电脑端）可同时登录
     * - 不同IP可同时登录
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public Result<UserInfo> login(@RequestBody Auth auth, HttpServletRequest request) {
        //1、验证码校验（开关由系统参数 captchaEnabled 控制，仅当显式为 false 时关闭，缺省按开启处理）
        String captchaError = validateCaptcha(auth, false);
        if (captchaError != null) {
            return Result.badRequest(captchaError);
        }
        //2、校验登录失败次数（用户名 + 来源 IP 双维度，任一超限即拒绝）
        String username = auth.getUserName();
        String password = auth.getPassword();
        String clientIp = IPUtil.getClientIpAddress(request);
        if (isLoginFailOverTimes(username, clientIp)) {
            return Result.badRequest("登录失败次数过多，请稍后再试！");
        }
        Auth sysUser = authService.getByUserName(username);
        //3、校验用户名密码
        if (!isLoginSuccess(password, sysUser)) {
            // 用户不存在与密码错误走完全相同的处理路径：双维度计数 + 统一错误提示，
            // 既防止针对不存在用户名的无限撞库，也消除两种情形在计数/锁定行为上的差异（防用户名枚举）
            addLoginFailOvertimes(username, clientIp);
            return Result.badRequest("用户名或密码错误！");
        }
        //3.1、校验账号状态（status=1正常，2冻结），冻结账号不允许登录
        if (sysUser.getStatus() == null || sysUser.getStatus() != 1) {
            return Result.badRequest("账号已被冻结，请联系管理员！");
        }
        //4. 获取终端类型，生成 token 并执行同IP踢下线逻辑（clientIp 已在上方取得）
        String deviceType = DeviceTypeUtil.detectDeviceType(request);
        UserInfo userInfo = this.getUserInfo(sysUser, clientIp, deviceType);

        return Result.success("登录成功，欢迎回来！", userInfo);
    }

    /**
     * 退出登录
     */
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public Result<String> logout(HttpServletRequest request) {
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
                // 清理本次会话及其 IP 索引（会话可能已被同 IP 新登录踢掉，方法内部做幂等处理）
                clearLogoutSession(username, token);
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
     * - 邮箱为必填（忘记密码时凭绑定邮箱接收验证码完成身份认证），且全系统账号内唯一
     * - 新账号 status=1（正常）、pwd_reset_required=0（密码由本人设置），不分配任何角色/部门/岗位，
     *   登录后仅可见无需权限的页面，由管理员另行授权
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public Result<String> register(@RequestBody Auth auth, HttpServletRequest request) {
        String userName = auth.getUserName() == null ? null : auth.getUserName().trim();
        String password = auth.getPassword();
        //1、格式校验（与前端校验规则保持一致，服务端为最终防线）
        String formatError = validateAccountFormat(userName, password);
        if (formatError != null) {
            return Result.badRequest(formatError);
        }
        String nickName = auth.getNickName();
        if (StringUtil.isNotEmpty(nickName) && nickName.trim().length() > 20) {
            return Result.badRequest("昵称长度不能超过20个字符！");
        }
        //邮箱必填、格式合法（找回密码依赖绑定邮箱做身份认证）
        String email = auth.getEmail() == null ? null : auth.getEmail().trim().toLowerCase();
        String emailError = validateEmailFormat(email);
        if (emailError != null) {
            return Result.badRequest(emailError);
        }
        String clientIp = IPUtil.getClientIpAddress(request);
        //2、IP 限流（滑块 token 仅 5 分钟单次有效，叠加窗口计数防批量注册）
        if (isSelfServiceOverTimes(AuthConst.PREFIX_REGISTER_TIMES_IP, clientIp)) {
            return Result.badRequest("操作过于频繁，请稍后再试！");
        }
        //3、滑块验证码校验
        String captchaError = validateCaptcha(auth, false);
        if (captchaError != null) {
            return Result.badRequest(captchaError);
        }
        //4、用户名查重（@TableLogic 自动排除已删除账号，删除后用户名可重新注册）
        if (authService.getByUserName(userName) != null) {
            return Result.badRequest("用户名已存在，请更换用户名！");
        }
        //5、邮箱查重：一个邮箱只能绑定一个正常账号（大小写不敏感），保证找回密码验证码有唯一归属
        if (authService.getOne(new QueryWrapper<Auth>().eq("email", email).last("LIMIT 1")) != null) {
            return Result.badRequest("该邮箱已被其他账号绑定，请更换邮箱！");
        }
        //6、组装账号：只设置白名单字段，其他字段（角色关联、头像、手机号等）一律不由注册接口写入
        Auth newUser = new Auth();
        newUser.setUserName(userName);
        newUser.setNickName(StringUtil.isNotEmpty(nickName) ? nickName.trim() : userName);
        newUser.setEmail(email);
        newUser.setPassword(authServiceCommon.encryptPassword(password));
        newUser.setStatus(CommonConst.STATUS_ENABLED);
        newUser.setPwdResetRequired(CommonConst.NO);
        newUser.setDelFlag(CommonConst.DEL_FLAG_NORMAL);
        authService.save(newUser);
        //限流计数在注册成功后写入
        addSelfServiceTimes(AuthConst.PREFIX_REGISTER_TIMES_IP, clientIp);
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
        if (isEmailSendIpLimited(clientIp)) {
            return Result.badRequest("验证码发送过于频繁，请稍后再试！");
        }
        //2、滑块验证码强制校验（发信场景即使系统关闭了登录验证码也必须验证）
        String captchaError = validateCaptcha(auth, true);
        if (captchaError != null) {
            return Result.badRequest(captchaError);
        }
        //3、查询账号及其绑定邮箱；不存在/未绑定邮箱一律走统一成功文案（不发信），防枚举
        Auth sysUser = authService.getByUserName(userName);
        if (sysUser != null && StringUtil.isNotEmpty(sysUser.getEmail())) {
            //同一账号 60 秒内已发过：直接返回统一提示，不重新生成/发送
            Object intervalFlag = redisUtil.get(AuthConst.PREFIX_PWD_EMAIL_INTERVAL_USER + userName);
            if (intervalFlag == null) {
                String code = generateEmailCode();
                redisUtil.set(AuthConst.PREFIX_PWD_EMAIL_CODE + userName, code,
                        AuthConst.PWD_EMAIL_CODE_TTL_SECONDS);
                //新验证码生效，历史错误计数清零，避免上一轮失败计数影响本轮
                redisUtil.del(AuthConst.PREFIX_PWD_EMAIL_CODE_FAIL + userName);
                //SMTP 未配置时 sendPasswordResetCode 返回 false（验证码只写日志），不影响接口契约
                mailSendService.sendPasswordResetCode(sysUser.getEmail(), code);
                redisUtil.set(AuthConst.PREFIX_PWD_EMAIL_INTERVAL_USER + userName, "1",
                        AuthConst.PWD_EMAIL_RESEND_INTERVAL_SECONDS);
            }
        }
        //4、IP 维度计数与间隔标记对"发信"与"未发信"请求都写入，防止用不存在用户名绕过频控
        redisUtil.set(AuthConst.PREFIX_PWD_EMAIL_INTERVAL_IP + clientIp, "1",
                AuthConst.PWD_EMAIL_RESEND_INTERVAL_SECONDS);
        redisUtil.incrWithExpire(AuthConst.PREFIX_PWD_EMAIL_TIMES_IP + clientIp,
                AuthConst.PWD_EMAIL_SEND_IP_WINDOW_SECONDS);
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
        //错误次数过多直接拒绝
        int failTimes = getLoginFailTimes(AuthConst.PREFIX_PWD_EMAIL_CODE_FAIL + userName);
        if (failTimes >= AuthConst.PWD_EMAIL_MAX_FAIL) {
            return Result.badRequest("验证码错误次数过多，请重新获取验证码！");
        }
        Object storedCode = redisUtil.get(AuthConst.PREFIX_PWD_EMAIL_CODE + userName);
        if (storedCode == null || !emailCode.equals(storedCode.toString())) {
            redisUtil.incrWithExpire(AuthConst.PREFIX_PWD_EMAIL_CODE_FAIL + userName,
                    AuthConst.PWD_EMAIL_FAIL_WINDOW_SECONDS);
            return Result.badRequest("验证码错误或已过期！");
        }
        //校验通过：验证码立即失效，签发一次性重置令牌（UUID 随机串，Redis 绑定用户名，10 分钟有效）
        redisUtil.del(AuthConst.PREFIX_PWD_EMAIL_CODE + userName);
        redisUtil.del(AuthConst.PREFIX_PWD_EMAIL_CODE_FAIL + userName);
        String resetToken = UUID.randomUUID().toString().replace("-", "");
        redisUtil.set(AuthConst.PREFIX_PWD_RESET_TOKEN + resetToken, userName,
                AuthConst.PWD_RESET_TOKEN_TTL_SECONDS);
        Map<String, String> data = new HashMap<>();
        data.put("resetToken", resetToken);
        return Result.success("验证成功，请设置新密码！", data);
    }

    /**
     * 找回密码第 3 步：凭一次性重置令牌设置新密码
     * - 令牌不存在/已使用/已过期一律拒绝；读取后立即删除（单次有效，防并发/重放）
     * - 重置成功后踢出该账号全部在线会话，防止旧令牌在改密后继续使用
     */
    @RequestMapping(value = "/password/reset", method = RequestMethod.POST)
    public Result<String> resetPassword(@RequestBody Auth auth) {
        String resetToken = auth.getResetToken();
        String newPassword = auth.getPassword();
        if (StringUtil.isEmpty(resetToken) || resetToken.length() > 64) {
            return Result.badRequest("重置凭证无效，请重新获取验证码！");
        }
        if (StringUtil.isEmpty(newPassword)
                || newPassword.length() < AuthConst.PASSWORD_MIN_LENGTH
                || newPassword.length() > AuthConst.PASSWORD_MAX_LENGTH) {
            return Result.badRequest("密码长度须为" + AuthConst.PASSWORD_MIN_LENGTH + "-" + AuthConst.PASSWORD_MAX_LENGTH + "个字符！");
        }
        String tokenKey = AuthConst.PREFIX_PWD_RESET_TOKEN + resetToken;
        Object usernameObj = redisUtil.get(tokenKey);
        if (usernameObj == null) {
            return Result.badRequest("重置凭证已失效，请重新获取验证码！");
        }
        //单次有效：先删令牌再执行后续操作，并发的第二个请求必然拿不到令牌
        redisUtil.del(tokenKey);
        String userName = usernameObj.toString();
        Auth sysUser = authService.getByUserName(userName);
        if (sysUser == null) {
            return Result.badRequest("账号状态异常，请联系管理员！");
        }
        //仅更新密码与强制改密标记（自助重置的密码本人已知，无需再强制改密），
        //使用精确 SET 避免实体空字段覆盖其他列
        LambdaUpdateWrapper<Auth> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Auth::getId, sysUser.getId())
                .set(Auth::getPassword, authServiceCommon.encryptPassword(newPassword))
                .set(Auth::getPwdResetRequired, 0);
        authService.update(updateWrapper);
        //改密即失效：踢出该账号全部在线会话（各 IP、各终端）
        kickOutAllSessions(userName);
        return Result.success("密码已重置，请使用新密码登录！");
    }

    /**
     * 获取用户信息（返回给前端）
     * 实际签发逻辑统一收敛到 {@link IAuthServiceCommon#issueLoginSession}，
     * 与"改密后免输入自动登录"复用同一套实现，避免两条链路行为漂移。
     */
    private UserInfo getUserInfo(Auth sysUser, String clientIp, String deviceType) {
        if (sysUser == null) {
            return new UserInfo();
        }
        Map<String, Object> data = authServiceCommon.issueLoginSession(
                sysUser.getUserName(), clientIp, deviceType);
        if (data == null) {
            return new UserInfo();
        }
        return JSON.parseObject(JSON.toJSONString(data), UserInfo.class);
    }

    /**
     * 退出登录的会话清理（幂等）：
     * <ol>
     *   <li>删除本次请求 token 的会话键（可能已被同 IP 新登录踢掉，DEL 天然幂等）；</li>
     *   <li>扫描该用户的"用户名-IP"索引，只删除两类：指向本次 token 的索引、以及指向为空/指向会话
     *       已不存在的孤儿索引。</li>
     * </ol>
     * 关键约束：<b>绝不能删除指向该用户其他存活会话的索引</b>。该索引是登录时"同 IP 重复登录
     * 踢旧保新"的唯一依据；误删后存活会话会变成无索引的孤儿，之后任何登录都踢不到它，
     * 只能等 JWT 自然过期，在线用户列表就会长期出现同一用户的多条会话。
     * <p>
     * 前缀碰撞（如 admin / admin_backup）安全：扫描可能命中其他用户的索引，但本方法只按
     * "索引指向的 token 值"判定——指向本次 token（必属当前用户）或已成孤儿时才删除，
     * 指向他人存活会话的索引一律保留。
     */
    private void clearLogoutSession(String username, String token) {
        if (StringUtil.isNotEmpty(token)) {
            redisUtil.del(AuthConst.PREFIX_USER_TOKEN + token);
        }
        Set<String> indexKeys = redisUtil.scanKeys(AuthConst.PREFIX_USER_BY_IP + username + "_*");
        if (indexKeys == null || indexKeys.isEmpty()) {
            return;
        }
        for (String indexKey : indexKeys) {
            Object indexedToken = redisUtil.get(indexKey);
            if (indexedToken == null) {
                // 孤儿索引：无值
                redisUtil.del(indexKey);
                continue;
            }
            String indexed = indexedToken.toString();
            // 指向本次退出的令牌：删除
            if (StringUtil.isNotEmpty(token) && token.equals(indexed)) {
                redisUtil.del(indexKey);
                continue;
            }
            // 指向的会话本体已不存在：孤儿索引，删除；指向其他存活会话的索引保留
            if (redisUtil.get(AuthConst.PREFIX_USER_TOKEN + indexed) == null) {
                redisUtil.del(indexKey);
            }
        }
    }

    /**
     * 校验滑块验证码（登录/注册/找回密码发信三处共用同一套校验逻辑）。
     *
     * @param forceEnabled true=无论系统参数 captchaEnabled 如何都强制校验（发邮件等敏感操作）；
     *                     false=仅当系统参数未显式设为 "false" 时校验
     * @return 校验失败返回错误提示；通过返回 null
     */
    private String validateCaptcha(Auth auth, boolean forceEnabled) {
        if (!forceEnabled) {
            String captchaEnabled = authService.getConfigValueByKey(AuthConst.CONFIG_CAPTCHA_ENABLED);
            if ("false".equalsIgnoreCase(captchaEnabled)) {
                return null;
            }
        }
        String captchaToken = auth.getCaptchaToken();
        if (StringUtil.isEmpty(captchaToken)) {
            return "请完成验证码验证！";
        }
        //校验签名与 captchaId，防止伪造 token
        boolean captchaValid;
        try {
            captchaValid = JwtUtil.verifyCaptchaToken(captchaToken, auth.getCaptchaId());
        } catch (Exception e) {
            captchaValid = false;
        }
        if (!captchaValid) {
            return "验证码验证失败，请重新验证！";
        }
        //单次有效：必须存在 /captcha/verify 时写入的 Redis 标记，校验通过立即删除，杜绝重放
        String captchaKey = AuthConst.PREFIX_CAPTCHA_TOKEN + captchaToken;
        if (redisUtil.get(captchaKey) == null) {
            return "验证码已失效或已被使用，请重新验证！";
        }
        redisUtil.del(captchaKey);
        return null;
    }

    /**
     * 用户名/密码格式校验（注册与找回密码共用），规则与前端登录页一致：
     * 用户名 3-10 个字符，密码 6-20 个字符
     *
     * @return 校验失败返回错误提示；通过返回 null
     */
    private String validateAccountFormat(String userName, String password) {
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

    /**
     * 注册/找回密码 IP 维度限流：窗口内计数达到阈值即拒绝
     */
    private boolean isSelfServiceOverTimes(String keyPrefix, String clientIp) {
        if (StringUtil.isEmpty(clientIp)) {
            return false;
        }
        return getLoginFailTimes(keyPrefix + clientIp) >= AuthConst.SELF_SERVICE_MAX_BY_IP;
    }

    /**
     * 邮箱格式校验（注册必填、统一小写后传入）
     *
     * @return 校验失败返回错误提示；通过返回 null
     */
    private String validateEmailFormat(String email) {
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

    /**
     * 找回密码发信 IP 频控：60 秒重发间隔 或 10 分钟窗口内累计达上限即拦截
     */
    private boolean isEmailSendIpLimited(String clientIp) {
        if (StringUtil.isEmpty(clientIp)) {
            return false;
        }
        if (redisUtil.get(AuthConst.PREFIX_PWD_EMAIL_INTERVAL_IP + clientIp) != null) {
            return true;
        }
        return getLoginFailTimes(AuthConst.PREFIX_PWD_EMAIL_TIMES_IP + clientIp)
                >= AuthConst.PWD_EMAIL_SEND_IP_MAX;
    }

    /**
     * 生成 6 位数字邮箱验证码（含前导零，使用 SecureRandom 防可预测）
     */
    private String generateEmailCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    /**
     * 写入注册/找回密码 IP 计数：原子自增，首次计数起开启滑动窗口
     */
    private void addSelfServiceTimes(String keyPrefix, String clientIp) {
        if (StringUtil.isNotEmpty(clientIp)) {
            redisUtil.incrWithExpire(keyPrefix + clientIp, AuthConst.SELF_SERVICE_WINDOW_SECONDS);
        }
    }

    /**
     * 踢出指定用户的全部在线会话：扫描该用户各 IP 索引，删除索引指向的 token 会话后再删索引。
     * 用于自助重置密码后让旧令牌立即失效。
     */
    private void kickOutAllSessions(String username) {
        Set<String> indexKeys = redisUtil.scanKeys(AuthConst.PREFIX_USER_BY_IP + username + "_*");
        if (indexKeys == null || indexKeys.isEmpty()) {
            return;
        }
        for (String indexKey : indexKeys) {
            Object indexedToken = redisUtil.get(indexKey);
            if (indexedToken == null) {
                //孤儿索引（指向的会话早已不存在），直接清理
                redisUtil.del(indexKey);
                continue;
            }
            String tokenKey = AuthConst.PREFIX_USER_TOKEN + indexedToken;
            Object metaObj = redisUtil.get(tokenKey);
            if (metaObj == null) {
                //会话本体已不存在，仅剩索引：按孤儿索引清理
                redisUtil.del(indexKey);
                continue;
            }
            //用会话元数据二次确认归属，避免前缀碰撞误踢其他用户（如 admin / admin_backup）；
            //旧结构（值即 token、无元数据）无法确认，按索引前缀归属处理
            boolean belongsToUser = true;
            if (metaObj instanceof String meta && meta.startsWith("{")) {
                try {
                    belongsToUser = username.equals(JSON.parseObject(meta).getString("username"));
                } catch (Exception ignored) {
                    belongsToUser = false;
                }
            }
            if (belongsToUser) {
                redisUtil.del(tokenKey);
                redisUtil.del(indexKey);
            }
        }
    }

    /**
     * 校验是否登录成功（纯校验，不做计数；失败计数由登录入口统一处理，保证用户不存在与密码错误路径一致）
     */
    private boolean isLoginSuccess(String password, Auth sysUser) {
        return sysUser != null && passwordEncoder.matches(password, sysUser.getPassword());
    }

    /**
     * 记录登录失败次数：用户名维度（保护账号）+ 来源 IP 维度（防轮换用户名撞库/枚举），
     * 原子自增，首次计数起 10 分钟滑动窗口，窗口内阈值见 {@link AuthConst}
     */
    private void addLoginFailOvertimes(String username, String clientIp) {
        redisUtil.incrWithExpire(AuthConst.PREFIX_LOGIN_FAIL_TIMES + username, AuthConst.LOGIN_FAIL_WINDOW_SECONDS);
        if (StringUtil.isNotEmpty(clientIp)) {
            redisUtil.incrWithExpire(AuthConst.PREFIX_LOGIN_FAIL_TIMES_IP + clientIp, AuthConst.LOGIN_FAIL_WINDOW_SECONDS);
        }
    }

    /**
     * 校验登录失败是否超限：用户名维度或来源 IP 维度任一达到阈值即锁定
     */
    private boolean isLoginFailOverTimes(String username, String clientIp) {
        return getLoginFailTimes(AuthConst.PREFIX_LOGIN_FAIL_TIMES + username) >= AuthConst.LOGIN_MAX_FAIL_BY_USERNAME
                || (StringUtil.isNotEmpty(clientIp)
                && getLoginFailTimes(AuthConst.PREFIX_LOGIN_FAIL_TIMES_IP + clientIp) >= AuthConst.LOGIN_MAX_FAIL_BY_IP);
    }

    /**
     * 读取失败计数；值缺失或异常均按 0 处理，绝不能因计数脏数据阻断正常登录
     */
    private int getLoginFailTimes(String key) {
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

    /**
     * 生成验证码
     */
    @GetMapping("/captcha/generate")
    public Result<Map<String, Object>> captchaGenerate() {
        String captchaId = UUID.randomUUID().toString();

        // 使用 CaptchaUtil 生成验证码图片
        Map<String, Object> captchaResult = CaptchaUtil.generateCaptcha();

        Map<String, Object> map = new HashMap<>();
        map.put("captchaId", captchaId);
        map.put("bgImage", captchaResult.get("bgImage"));
        map.put("sliderImage", captchaResult.get("sliderImage"));
        map.put("sliderTop", captchaResult.get("offsetY"));

        // 只存储正确位置，不返回给前端
        redisUtil.set("captcha:" + captchaId, String.valueOf(captchaResult.get("offsetX")), 300);

        return Result.success(map);
    }

    /**
     * 校验验证码
     */
    @PostMapping("/captcha/verify")
    public Result<Map<String, Object>> captchaVerify(HttpServletRequest request, @RequestBody JSONObject jsonObject) {
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
        String captchaId = jsonObject.getString("captchaId");
        Double captchaOffset = jsonObject.getDouble("captchaOffset");
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
