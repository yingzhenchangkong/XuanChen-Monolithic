package com.xuanchen.auth.shiro.realm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.mapper.AuthMapper;
import com.xuanchen.auth.auth.service.IAuthService;
import com.xuanchen.auth.shiro.config.JwtToken;
import com.xuanchen.auth.shiro.exception.JwtAuthException;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.utils.RedisUtil;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

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

    @Autowired
    private AuthMapper authMapper;

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
     * 从数据库加载当前登录用户的角色编码（sys_role.role_code，如 admin/common）
     * 与菜单权限标识（sys_menu.perms），供 @RequiresRoles/@RequiresPermissions
     * 以及过滤器链中的 roles[xxx] 判定使用。
     *
     * @param principalCollection
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        Auth authUser = (Auth) principalCollection.getPrimaryPrincipal();
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        if (authUser == null || authUser.getUserName() == null || authUser.getUserName().isEmpty()) {
            return authorizationInfo;
        }
        List<String> roleCodes = authMapper.listRoleCodesByUserName(authUser.getUserName());
        if (roleCodes != null) {
            authorizationInfo.addRoles(roleCodes);
        }
        List<String> permissions = authMapper.listPermsByUserName(authUser.getUserName());
        if (permissions != null) {
            authorizationInfo.addStringPermissions(permissions);
        }
        return authorizationInfo;
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
        if (token == null || token.isBlank()) {
            throw new JwtAuthException(JwtAuthException.Reason.TOKEN_EMPTY);
        }
        // 2、验证 token 有效性
        //  2.1、获取 username 用于和数据库对比
        String username = JwtUtil.getUsername(token);
        if (username == null || username.isBlank()) {
            throw new JwtAuthException(JwtAuthException.Reason.TOKEN_MALFORMED);
        }
        //  2.2、查询用户信息
        //  注意：Auth.delFlag 已配置 @TableLogic，自动查询会追加 del_flag = 0；
        //  下面仍保留显式 delFlag 校验作为纵深防御，同时必须显式校验 status——
        //  否则已冻结用户在 token 有效期内仍可携带旧令牌访问
        LambdaQueryWrapper<Auth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Auth::getUserName, username);
        Auth authUser = authService.getOne(queryWrapper);
        if (authUser == null
                || (authUser.getDelFlag() != null && authUser.getDelFlag() != 0)) {
            //用户不存在与已删除合并为同一对外提示，避免账号枚举
            throw new JwtAuthException(JwtAuthException.Reason.USER_NOT_FOUND);
        }
        if (authUser.getStatus() == null || authUser.getStatus() != 1) {
            //管理员冻结后必须在下一次请求立即生效，不能仅依赖 token 自身过期
            throw new JwtAuthException(JwtAuthException.Reason.USER_FROZEN);
        }
        // 2.3、验证token是否超时失效 未失效时更新有效期为新时间
        //  直接取 Object 判空，禁止 String.valueOf(...)——null 会被转成字符串 "null"，
        //  导致判空条件永不成立（死代码），令牌在 Redis 中失效后只能靠后续验签兜底
        Object cachedToken = redisUtil.get(AuthConst.PREFIX_USER_TOKEN + token);
        if (cachedToken == null) {
            throw new JwtAuthException(JwtAuthException.Reason.SESSION_EXPIRED);
        }
        String redisToken = String.valueOf(cachedToken);
        //区分 JWT 过期与签名/篡改：过期提示用户重新登录，签名异常同时服务端告警
        JwtUtil.VerifyFailure verifyFailure =
                JwtUtil.verifyFailureReason(redisToken, username);
        if (verifyFailure != null) {
            throw new JwtAuthException(verifyFailure == JwtUtil.VerifyFailure.TOKEN_EXPIRED
                    ? JwtAuthException.Reason.TOKEN_EXPIRED
                    : JwtAuthException.Reason.SIGNATURE_INVALID);
        }
        // 3、验证密码，我们可以使用一个AuthenticationInfo实现类 SimpleAuthenticationInfo shiro会自动验证
        return new SimpleAuthenticationInfo(authUser, token, getName());
    }
}
