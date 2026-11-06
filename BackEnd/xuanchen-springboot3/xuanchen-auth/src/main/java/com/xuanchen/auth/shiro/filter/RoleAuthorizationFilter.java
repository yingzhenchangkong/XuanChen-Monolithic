package com.xuanchen.auth.shiro.filter;

import com.xuanchen.auth.utils.JwtUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.RolesAuthorizationFilter;

import java.io.IOException;

/**
 * 角色授权过滤器：Shiro 内置 RolesAuthorizationFilter 在角色不满足时默认走 302
 * 重定向到登录页（页面场景），与本项目前后端分离、统一 JSON 响应的约定不符。
 * 此处覆盖拒绝行为：未认证返回 401，已认证但角色不足返回 403 JSON。
 *
 * @author XuanChen
 * @date 2026-09-29
 */
public class RoleAuthorizationFilter extends RolesAuthorizationFilter {
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws IOException {
        Subject subject = getSubject(request, response);
        if (subject.isAuthenticated()) {
            JwtUtil.responseError(response, 403, "权限不足，禁止访问！");
        } else {
            JwtUtil.responseError(response, 401, "Token失效，请重新登录!");
        }
        return false;
    }
}
