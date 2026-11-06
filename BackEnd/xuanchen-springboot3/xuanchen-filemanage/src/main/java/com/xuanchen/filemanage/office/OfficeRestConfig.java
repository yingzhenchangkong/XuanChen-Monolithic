package com.xuanchen.filemanage.office;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 配置-->ONLYOFFICE 专用 RestTemplate（回调拉取文档用，独立超时）
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Configuration
public class OfficeRestConfig {

    @Bean
    public RestTemplate officeRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return new RestTemplate(factory);
    }
}
