package com.xuanchen.filemanage.office;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 配置-->ONLYOFFICE 协同编辑（xuanchen.office 前缀）
 * <p>ds-url：Document Server 地址（Windows 直装默认端口 80 可改 8082 等，须与前端同源可达）；
 * jwt-secret：与 Document Server config 的 JWT 密钥一致（两端共用同一密钥互签）；
 * public-base-url：Document Server 回调/拉取文档时访问本后端的基地址（DS 与后端同机默认 localhost:18080）。</p>
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Data
@Component
@ConfigurationProperties(prefix = "xuanchen.office")
public class OfficeProperties {

    /** Document Server 基地址（前端加载 api.js 与打开编辑器均用此地址） */
    private String dsUrl = "http://localhost:8082";

    /** JWT 密钥：与 Document Server 的 JWT_SECRET 配置保持一致 */
    private String jwtSecret = "xc-office-secret-change-me";

    /** Document Server 访问本后端可用的基地址（拼 /office/download、/office/callback） */
    private String publicBaseUrl = "http://localhost:18080";
}
