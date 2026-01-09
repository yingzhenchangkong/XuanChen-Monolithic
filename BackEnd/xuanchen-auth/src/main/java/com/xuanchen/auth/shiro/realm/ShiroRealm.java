package com.xuanchen.auth.shiro.realm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.service.IAuthService;
import com.xuanchen.auth.shiro.config.JwtToken;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.utils.RedisUtil;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ShiroRealm
 *
 * @author XuanChen
 * @date 2025-03-12
 */
public class ShiroRealm extends AuthorizingRealm {
    @Autowired
    private IAuthService authService;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 必须重写此方法，不然Shiro会报错
     * 每一个Ream都有一个supports方法，用于检测是否支持此Token ，如果没有重写supports就会报错 does not support authentication token
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    /**
     * 授权
     *
     * @param principalCollection
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        return null;
    }

    /**
     * 认证
     *
     * @param authenticationToken
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        //1、获取 token
        String token = (String) authenticationToken.getCredentials();
        if (token == null) {
            throw new AuthenticationException("token为空!");
        }
        // 2、验证 token 有效性
        //  2.1、获取 username 用于和数据库对比
        String username = JwtUtil.getUsername(token);
        if (username == null) {
            throw new AuthenticationException("token非法无效!");
        }
        //  2.2、查询用户信息
        LambdaQueryWrapper<Auth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Auth::getUserName, username);
        Auth authUser = authService.getOne(queryWrapper);
        if (authUser == null) {
            throw new AuthenticationException("用户不存在!");
        }
        // 2.3、验证token是否超时失效 未失效时更新有效期为新时间
        String redisToken = String.valueOf(redisUtil.get(AuthConst.PREFIX_USER_TOKEN + token));
        if (redisToken == null) {
            throw new AuthenticationException("Token失效，请重新登录!");
        }
        if (!JwtUtil.verify(redisToken, username, authUser.getPassword())) {
            throw new AuthenticationException("Token验证失败!");
        }
        // 3、验证密码，我们可以使用一个AuthenticationInfo实现类 SimpleAuthenticationInfo shiro会自动验证
        SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo(authUser, token, getName());
        return simpleAuthenticationInfo;
    }
}
