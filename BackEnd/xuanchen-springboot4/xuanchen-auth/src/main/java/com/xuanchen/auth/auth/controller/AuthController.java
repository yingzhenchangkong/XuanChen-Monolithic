package com.xuanchen.auth.auth.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.entity.UserInfo;
import com.xuanchen.auth.auth.service.IAuthService;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private PasswordEncoder passwordEncoder;

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public Result<UserInfo> login(@RequestBody Auth auth) {
        //1.如果有验证码token，先验证
        if (StringUtil.isNotEmpty(auth.getCaptchaToken())) {
            try {
                if (!JwtUtil.verifyCaptchaToken(auth.getCaptchaToken(), auth.getCaptchaId())) {
                    return Result.badRequest("验证码验证失败！");
                }
            } catch (Exception e) {
                return Result.badRequest("验证码验证失败！");
            }
        } else {
            return Result.badRequest("验证码验证失败！");
        }
        //2.执行登录逻辑
        String username = auth.getUserName();
        String password = auth.getPassword();
        Auth sysUser = this.getUserInfo(username);
        //校验登录失败次数
        if (isLoginFailOverTimes(username)) {
            return Result.badRequest("登录失败次数过多，请稍后再试！");
        }
        //校验用户是否存在，校验用户名密码是否正确
        if (!isLoginSucc(username, password, sysUser)) {
            return Result.badRequest("用户名或密码错误！");
        }
        //获取用户信息
        UserInfo userInfo = this.getUserInfo(sysUser);
        //3.返回登录结果
        return Result.success("登录成功，欢迎回来！", userInfo);
    }

    /**
     * 退出登录
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public Result<String> logout(HttpServletRequest request) {
        String token = request.getHeader(AuthConst.XC_ACCESS_TOKEN);
        if (StringUtil.isEmpty(token)) {
            return Result.badRequest("退出登录失败！");//无token
        }
        String username = JwtUtil.getUsername(token);
        if (StringUtil.isEmpty(username)) {
            return Result.badRequest("退出登录失败！");//token失效
        } else {
            Auth sysUser = this.getUserInfo(username);
            if (sysUser != null) {
                redisUtil.del(AuthConst.PREFIX_USER_TOKEN + token);
                SecurityContextHolder.clearContext();
                return Result.success("退出登录成功！");
            } else {
                return Result.badRequest("退出登录失败！");
            }
        }
    }

    /**
     * 获取用户信息(后台自用)
     *
     * @param username
     * @return
     */
    private Auth getUserInfo(String username) {
        LambdaQueryWrapper<Auth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Auth::getUserName, username).eq(Auth::getDelFlag, 0);
        return authService.getOne(queryWrapper);
    }

    /**
     * 获取用户信息(返回前台)
     *
     * @param sysUser
     * @return
     */
    private UserInfo getUserInfo(Auth sysUser) {
        //定义用户信息
        UserInfo userInfo = new UserInfo();
        if (sysUser != null) {
            String userName = sysUser.getUserName();
            //生成token
            String token = JwtUtil.sign(userName, sysUser.getPassword());//注意：此处密码应为加密后密码，否则会报401
            //token写入redis 保存 2 天
            redisUtil.set(AuthConst.PREFIX_USER_TOKEN + token, token, JwtUtil.EXPIRE_TIME * 2 / 1000);
            //删除登录失败信息
            redisUtil.del(AuthConst.PREFIX_LOGIN_FAIL_TIMES + userName);
            //用户信息赋值
            userInfo.setId(sysUser.getId());
            userInfo.setUserName(userName);
            userInfo.setNickName(sysUser.getNickName());
            userInfo.setMobile(sysUser.getMobile());
            userInfo.setEmail(sysUser.getEmail());
            userInfo.setAvatar(sysUser.getAvatar());
            userInfo.setToken(token);
        }
        return userInfo;
    }

    /**
     * 校验是否登录成功
     *
     * @param username 用户名
     * @param password 密码
     * @return 成功-->true 失败-->false
     */
    private boolean isLoginSucc(String username, String password, Auth sysUser) {
        //判断用户是否存在
        if (sysUser == null) {
            return false;
        }
        //判断用户名密码是否正确
        if (!passwordEncoder.matches(password, sysUser.getPassword())) {
            addLoginFailOvertimes(username);
            return false;
        }
        return true;
    }

    /**
     * 记录登录失败次数
     *
     * @param username
     */
    private void addLoginFailOvertimes(String username) {
        String key = AuthConst.PREFIX_LOGIN_FAIL_TIMES + username;
        Object failTimes = redisUtil.get(key);
        Integer val = 0;
        if (failTimes != null) {
            val = Integer.parseInt(failTimes.toString());
        }
        redisUtil.set(key, String.valueOf(++val), 10 * 60);//10分钟过期
    }

    /**
     * 校验登录失败是否超出 5 次
     *
     * @param username
     * @return 超出5次返回true 否则返回false
     */
    private boolean isLoginFailOverTimes(String username) {
        String key = AuthConst.PREFIX_LOGIN_FAIL_TIMES + username;
        Object failTimes = redisUtil.get(key);
        if (failTimes != null) {
            Integer redisFailTimes = Integer.parseInt(failTimes.toString());
            if (redisFailTimes >= 5) {
                return true;
            }
        }
        return false;
    }

    @GetMapping("/captcha/generate")
    public Result<Map<String, Object>> captchaGenerate() {
        String captchaId = UUID.randomUUID().toString();
        Double captchaOffset = Math.floor((Math.random() * (235 - 75) + 75) * 10000) / 10000.0;
        Map<String, Object> map = new HashMap<>();
        map.put("captchaId", captchaId);
        map.put("captchaOffset", captchaOffset);

        redisUtil.set("captcha:" + captchaId, String.valueOf(captchaOffset), 300);
        return Result.success(map);
    }

    @PostMapping("/captcha/verify")
    public Result<Map<String, Object>> captchaVerify(@RequestBody JSONObject jsonObject) {
        String captchaId = jsonObject.getString("captchaId");
        Double captchaOffset = jsonObject.getDouble("captchaOffset");
        // 1. 检查captchaId是否存在且未过期
        Object storedCaptchaInfo = redisUtil.get("captcha:" + captchaId);
        if (storedCaptchaInfo == null) {
            return Result.error("验证失败，请重新验证！");//验证码不存在或已过期
        }
        // 2. 检查偏移量是否正确
        Double storedCaptchaOffset = Double.parseDouble(storedCaptchaInfo.toString());
        if (Math.abs(captchaOffset - storedCaptchaOffset) > 5) {
            return Result.error("验证失败，请重新验证！");//验证码偏移量错误
        }
        // 3. 生成后端验证token 5分钟有效期
        String captchaToken = JwtUtil.createCaptchaToken(captchaId, 300);
        // 4. 删除已使用的验证码
        redisUtil.del("captcha:" + captchaId);
        // 5. 返回验证token
        Map<String, Object> map = new HashMap<>();
        map.put("captchaToken", captchaToken);
        return Result.success(map);
    }
}
