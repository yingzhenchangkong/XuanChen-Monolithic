package com.xuanchen.generator.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 启动阶段初始化数据源密码 AES 密钥。
 * <p>
 * 密钥配置项 xuanchen.crypto.datasource-key：
 * <ul>
 *   <li>dev profile：application-dev.yml 提供仅本地使用的默认值；</li>
 *   <li>prod profile：application-prod.yml 以 ${XUANCHEN_DATASOURCE_SECRET} 无默认值引用，
 *       未配置时 Spring 占位符解析即失败（fail-fast）。</li>
 * </ul>
 *
 * @author XuanChen
 * @date 2026-09-24
 */
@Component
public class DatasourceCryptoInitializer {

    public DatasourceCryptoInitializer(@Value("${xuanchen.crypto.datasource-key:}") String datasourceKey) {
        DatasourceCipher.init(datasourceKey);
    }
}
