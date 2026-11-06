package com.xuanchen.auth.service;

import com.xuanchen.common.utils.StringUtil;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务（忘记密码邮箱验证码通道）
 * <p>
 * 设计要点：
 * - 未配置 spring.mail.host 时 Spring Boot 不会创建 JavaMailSender（其自动配置以 host 存在为条件），
 *   这里用 {@link ObjectProvider} 解析，缺失也不影响应用启动；
 * - 未配置 SMTP 时邮件降级为仅写日志，方便本地开发/联调验证码链路；
 *   生产环境必须配置 SPRING_MAIL_* 环境变量，否则用户永远收不到验证码。
 *
 * @author XuanChen
 * @date 2026-09-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailSendService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${xuanchen.mail.from:}")
    private String mailFrom;

    /**
     * SMTP 是否已经可用（host 与用户名均已配置、且 JavaMailSender Bean 存在）
     */
    public boolean isMailEnabled() {
        return StringUtil.isNotEmpty(mailHost)
                && StringUtil.isNotEmpty(mailUsername)
                && mailSenderProvider.getIfAvailable() != null;
    }

    /**
     * 发送找回密码验证码。
     *
     * @param to   收件人邮箱（账号绑定邮箱）
     * @param code 6 位数字验证码
     * @return true=已通过 SMTP 发出；false=SMTP 未配置，验证码仅输出到日志（开发降级）
     */
    public boolean sendPasswordResetCode(String to, String code) {
        if (!isMailEnabled()) {
            //未配置 SMTP：绝不阻断业务，但以 WARN 明确提示该通道未真正发信，避免误以为用户能收到
            log.warn("[找回密码] SMTP 未配置（spring.mail.host 为空），验证码仅输出到日志（仅限开发联调）：to={}, code={}", to, code);
            return false;
        }
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(to);
            message.setSubject("【萱晨管理系统】找回密码验证码");
            message.setText("您正在进行找回密码操作，验证码：" + code
                    + "，5 分钟内有效。请勿向任何人泄露验证码；如非本人操作，请忽略本邮件。");
            mailSender.send(message);
            log.info("[找回密码] 验证码邮件已发送至 {}", to);
            return true;
        } catch (Exception e) {
            //发送失败不向调用方抛出可区分账号的信息，由上层统一提示；日志保留诊断信息
            log.error("[找回密码] 验证码邮件发送失败：to={}, error={}", to, e.getMessage(), e);
            throw new IllegalStateException("邮件发送失败，请稍后重试或联系管理员");
        }
    }

    /**
     * 解析发件人：优先 xuanchen.mail.from；未配置时回退到 SMTP 登录用户名。
     * 支持“昵称 <addr>”写法，非法地址直接退回原始值交由底层校验。
     */
    private String resolveFromAddress() {
        if (StringUtil.isNotEmpty(mailFrom)) {
            return mailFrom;
        }
        try {
            InternetAddress address = new InternetAddress(mailUsername);
            address.setPersonal("萱晨管理系统", "UTF-8");
            return address.toString();
        } catch (Exception e) {
            return mailUsername;
        }
    }
}
