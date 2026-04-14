package com.xuanchen.auth.auth.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
    public Result login(@RequestBody Auth auth, HttpServletRequest request) {
        String username = auth.getUserName();
        String password = auth.getPassword();
        Auth sysUser = this.getUserInfo(username);
        //校验登录失败次数
        if (isLoginFailOverTimes(username)) {
            return Result.error("登录失败次数过多，请稍后再试！");
        }
        //校验用户是否存在，校验用户名密码是否正确
        if (!isLoginSucc(username, password, sysUser)) {
            return Result.error("用户名或密码错误！");
        }
        //返回用户信息
        UserInfo userInfo = this.getUserInfo(sysUser);
        return Result.success("登录成功，欢迎回来！", userInfo);
    }

    /**
     * 退出登录
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader(AuthConst.XC_ACCESS_TOKEN);
        if (StringUtil.isEmpty(token)) {
            return Result.error("退出登录失败！");//无token
        }
        String username = JwtUtil.getUsername(token);
        if (StringUtil.isEmpty(username)) {
            return Result.error("退出登录失败！");//token失效
        } else {
            Auth sysUser = this.getUserInfo(username);
            if (sysUser != null) {
                redisUtil.del(AuthConst.PREFIX_USER_TOKEN + token);
                SecurityContextHolder.clearContext();
                return Result.success("退出登录成功！");
            } else {
                return Result.error("退出登录失败！");
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
}
