package com.xuanchen.auth.config;

import com.xuanchen.auth.security.JwtAccessDeniedHandler;
import com.xuanchen.auth.security.JwtAuthenticationEntryPoint;
import com.xuanchen.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 安全配置类
 *
 * @author XuanChen
 * @date 2026-03-20
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 允许跨域访问的来源（精确 Origin，逗号分隔），由 application-dev/prod.yml 或环境变量
     * XUANCHEN_CORS_ALLOWED_ORIGINS 提供。allowCredentials=true 时禁止使用 "*"，
     * 未配置任何来源则跨域请求一律不返回 CORS 头（fail-closed，同源/curl 不受影响）。
     */
    @Value("${xuanchen.cors.allowed-origins:}")
    private String allowedOrigins;

    /**
     * 密码编码器已迁至 {@link PasswordConfig}：避免业务层 PasswordCodec 经
     * PasswordEncoder 回指持有过滤器的 SecurityConfig 而形成 Bean 构造循环
     */

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 启用 CORS
                //禁用 Spring Security 默认的 logout
                .logout((logout -> logout.disable()))
                // 配置权限规则
                .authorizeHttpRequests(authorize -> authorize
                        //公开接口
                        .requestMatchers("/login").permitAll()
                        //自助注册（接口内部做滑块验证+IP限流+邮箱/用户名查重）
                        .requestMatchers("/register").permitAll()
                        //找回密码三步公开接口：发送邮箱验证码/校验验证码换令牌/凭令牌重置
                        //（发信强制滑块+IP频控，重置凭一次性令牌，均不暴露账号是否存在）
                        .requestMatchers("/password/**").permitAll()
                        //刷新令牌：凭 httpOnly Cookie 中的刷新令牌签发新访问令牌，无需认证
                        .requestMatchers("/auth/refresh").permitAll()
                        //验证码接口
                        .requestMatchers("/captcha/**").permitAll()
                        //系统配置（登录页按key读取公开配置项）
                        .requestMatchers("/system/config/getConfigKeyValue").permitAll()
                        //头像/图片静态资源：浏览器 <img src> 无法携带自定义鉴权头，仅放行 GET 预览/下载；
                        //上传(POST)仍需认证。路径穿越已由 FileSecurityUtil 规范化 containment 校验拦截，
                        //新文件名为服务端随机 UUID，不能通过猜测枚举
                        .requestMatchers(HttpMethod.GET, "/filemanage/static/**").permitAll()
                        //文件共享公开访问：shareNo 为 32 位随机 UUID 不可枚举，可选提取码，
                        //撤销/过期/源文件删除即时失效，下载为现签预签名 URL（详见 FileShareServiceImpl）
                        .requestMatchers("/share/**").permitAll()
                        //数据接口服务公开分发器：/open/api/{code}，接口级鉴权由
                        //api.api_key（X-Api-Key 头/apiKey 参数）自行控制，未配置密钥即内部免鉴权
                        .requestMatchers("/open/api/**").permitAll()
                        //ONLYOFFICE 协同编辑：文档下发（短时效签名令牌）与保存回调（回调体验签）
                        .requestMatchers("/office/download").permitAll()
                        .requestMatchers("/office/callback").permitAll()
                        //WebSocket
                        .requestMatchers("/ws").permitAll()
                        //管理类URL前缀：仅管理员角色可访问（方法级 @PreAuthorize 之外的第二道防线）
                        .requestMatchers(
                                "/monitor/**",
                                //代码生成器（数据源/表管理/代码生成）：与三个 Controller 类级
                                // @PreAuthorize("hasRole('admin')") 形成双层鉴权
                                "/tool/**",
                                "/codeGenerator/**",
                                "/system/role/**",
                                "/system/dept/**",
                                "/system/post/**",
                                "/system/accountrole/**",
                                "/system/persondept/**",
                                "/system/personpost/**",
                                "/system/rolemenu/**"
                        ).hasRole("admin")
                        //其他请求需要认证
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        //仅允许显式配置的精确来源（开发/生产各自配置，禁止 "*" + credentials 组合）
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (!origins.isEmpty()) {
            config.setAllowedOrigins(origins);
        }

        // 允许所有请求头
        config.addAllowedHeader("*");

        // 允许所有请求方法
        config.addAllowedMethod("*");

        // 允许携带凭证（XC-ACCESS-TOKEN 走自定义头，保留以兼容未来 Cookie 场景）
        config.setAllowCredentials(true);

        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
