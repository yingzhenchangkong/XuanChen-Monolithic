package com.xuanchen.common.config;

import com.xuanchen.common.utils.IPUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 可信反向代理网段初始化器
 * <p>
 * 把 xuanchen.security.trusted-proxies（环境变量 XUANCHEN_TRUSTED_PROXIES，逗号分隔 CIDR）
 * 注入 {@link IPUtil} 的静态可信网段。未配置时 IPUtil 只采信 TCP 连接对端 RemoteAddr，
 * 任何 X-Forwarded-For / X-Real-IP 均被忽略。
 *
 * @author XuanChen
 * @date 2026-09-18
 */
@Slf4j
@Component
public class TrustedProxyInitializer {

    @Value("${xuanchen.security.trusted-proxies:}")
    private String trustedProxies;

    @PostConstruct
    public void init() {
        IPUtil.initTrustedProxies(trustedProxies);
        if (trustedProxies == null || trustedProxies.isBlank()) {
            log.warn("未配置 xuanchen.security.trusted-proxies（XUANCHEN_TRUSTED_PROXIES）："
                    + "将只按 TCP 对端 IP 识别客户端，X-Forwarded-For 等代理头一律忽略；"
                    + "若服务部署在 Nginx 等反向代理之后，请把代理自身网段配置为可信代理");
        }
    }
}
