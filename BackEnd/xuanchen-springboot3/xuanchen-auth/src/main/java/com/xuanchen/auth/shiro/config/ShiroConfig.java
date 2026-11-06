package com.xuanchen.auth.shiro.config;

import com.xuanchen.auth.config.CorsProperties;
import com.xuanchen.auth.shiro.filter.JwtFilter;
import com.xuanchen.auth.shiro.filter.RoleAuthorizationFilter;
import com.xuanchen.auth.shiro.realm.ShiroRealm;
import jakarta.servlet.Filter;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shiro 配置类
 *
 * @author XuanChen
 * @date 2025-03-12
 */
@Configuration
public class ShiroConfig {
    /**
     * 管理员角色编码，对应 sys_role.role_code
     */
    private static final String ROLE_ADMIN = "admin";

    @Bean(name = "shiroRealm")
    public ShiroRealm shiroRealm() {
        return new ShiroRealm();
    }

    @Bean(name = "securityManager")
    public DefaultWebSecurityManager getDefaultSecurityManager(@Qualifier("shiroRealm") ShiroRealm shiroRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(shiroRealm);//绑定自定义Realm
        return securityManager;
    }

    /**
     * 开启 Shiro 注解（@RequiresRoles/@RequiresPermissions 等）支持，
     * 使 Controller 方法级鉴权注解生效
     */
    @Bean
    public DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
        advisorAutoProxyCreator.setProxyTargetClass(true);
        return advisorAutoProxyCreator;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(
            @Qualifier("securityManager") DefaultWebSecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

    @Bean(name = "shiroFilterFactoryBean")
    public ShiroFilterFactoryBean getShiroFilterFactoryBean(@Qualifier("securityManager") DefaultWebSecurityManager securityManager,
                                                            CorsProperties corsProperties) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();

        shiroFilterFactoryBean.setSecurityManager(securityManager);

        Map<String, Filter> filterMap = new HashMap<>();
        //JwtFilter 非 Spring Bean（Shiro 过滤器需手工实例化注册），
        //CORS 白名单由容器注入本方法的 corsProperties 单例经构造器传入
        filterMap.put("jwt", new JwtFilter(corsProperties));
        //覆盖 Shiro 内置 roles 过滤器，角色不足时返回 403 JSON 而非 302 重定向
        filterMap.put("roles", new RoleAuthorizationFilter());
        shiroFilterFactoryBean.setFilters(filterMap);

        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
        //匿名接口
        filterChainDefinitionMap.put("/login", "anon");//登录
        filterChainDefinitionMap.put("/register", "anon");//自助注册
        filterChainDefinitionMap.put("/password/**", "anon");//找回密码（发验证码/校验/重置）
        filterChainDefinitionMap.put("/captcha/**", "anon");//滑块验证码
        filterChainDefinitionMap.put("/filemanage/static/**", "anon");//文件路径
        filterChainDefinitionMap.put("/share/**", "anon");//文件共享公开访问（shareNo 随机不可枚举，可选提取码）
        filterChainDefinitionMap.put("/office/download", "anon");//ONLYOFFICE 文档下发（短时效签名令牌校验）
        filterChainDefinitionMap.put("/office/callback", "anon");//ONLYOFFICE 保存回调（回调体验签）
        filterChainDefinitionMap.put("/open/api/**", "anon");//数据接口服务公开分发器（接口级鉴权由 api.api_key 控制）
        filterChainDefinitionMap.put("/system/config/getConfigKeyValue", "anon");//验证码开关
        filterChainDefinitionMap.put("/ws/**", "anon");//websocket
        filterChainDefinitionMap.put("/ws", "anon");//websocket
        filterChainDefinitionMap.put("/auth/refresh", "anon");//刷新令牌（凭 httpOnly Cookie 签发新访问令牌）

        //管理类URL前缀：仅管理员可访问（方法级 @RequiresRoles 之外的第二道防线，按首次匹配生效）
        filterChainDefinitionMap.put("/system/dict/select", "jwt");//字典下拉：登录即可用
        String adminChain = "jwt,roles[" + ROLE_ADMIN + "]";
        filterChainDefinitionMap.put("/monitor/**", adminChain);//服务监控/缓存/在线用户/日志
        filterChainDefinitionMap.put("/tool/**", adminChain);//代码生成器（数据源/表管理）
        filterChainDefinitionMap.put("/codeGenerator/**", adminChain);//代码生成
        filterChainDefinitionMap.put("/system/role/**", adminChain);//角色管理
        filterChainDefinitionMap.put("/system/dept/**", adminChain);//部门管理
        filterChainDefinitionMap.put("/system/post/**", adminChain);//岗位管理
        filterChainDefinitionMap.put("/system/accountrole/**", adminChain);//用户-角色关系
        filterChainDefinitionMap.put("/system/persondept/**", adminChain);//用户-部门关系
        filterChainDefinitionMap.put("/system/personpost/**", adminChain);//用户-岗位关系
        filterChainDefinitionMap.put("/system/rolemenu/**", adminChain);//角色-菜单关系
        filterChainDefinitionMap.put("/system/dict/**", adminChain);//字典管理（/select 已在上方放行给登录用户）
        filterChainDefinitionMap.put("/system/config/**", adminChain);//参数配置（getConfigKeyValue 已在上方匿名放行）

        filterChainDefinitionMap.put("/**", "jwt");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);

        return shiroFilterFactoryBean;
    }
}
