package com.scanplatform.service;

import com.scanplatform.entity.SysConfig;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Properties;

/**
 * 根据 active_scan_mail（实体 SysConfig）动态 SMTP 发送告警/主动扫描通知邮件。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertMailService {

    /**
     * @return true 表示已尝试发送且 SMTP 返回成功
     */
    public boolean sendMail(SysConfig config, String toAddresses, String subject, String text) {
        return sendFailureAlert(config, toAddresses, subject, text);
    }

    public boolean sendFailureAlert(SysConfig config, String toAddresses, String subject, String text) {
        if (!StringUtils.hasText(toAddresses)) {
            log.warn("未配置告警邮箱，跳过邮件发送");
            return false;
        }
        if (!StringUtils.hasText(config.getSmtpHost()) || config.getSmtpPort() == null) {
            log.warn("未配置 SMTP 主机或端口，跳过邮件发送");
            return false;
        }
        String[] to = Arrays.stream(toAddresses.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
        if (to.length == 0) {
            log.warn("告警邮箱解析后为空（请检查逗号分隔格式），跳过邮件发送 raw={}", toAddresses);
            return false;
        }
        JavaMailSenderImpl sender = buildSender(config);
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String from = StringUtils.hasText(config.getSmtpUsername()) ? config.getSmtpUsername() : "noreply@localhost";
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            log.info(
                    "SMTP 发送邮件: host={} port={} ssl={} tls={} from={} toCount={} subject={} bodyChars={}",
                    config.getSmtpHost(),
                    config.getSmtpPort(),
                    config.getSmtpSslEnabled(),
                    config.getSmtpTlsEnabled(),
                    from,
                    to.length,
                    subject,
                    text != null ? text.length() : 0);
            long t0 = System.currentTimeMillis();
            try {
                sender.send(message);
            } finally {
                log.info("SMTP send() 结束: subject={} 耗时Ms={}", subject, System.currentTimeMillis() - t0);
            }
            log.info("SMTP 邮件已提交: subject={}", subject);
            return true;
        } catch (Exception e) {
            log.error("发送告警邮件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private JavaMailSenderImpl buildSender(SysConfig c) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(c.getSmtpHost());
        int port = c.getSmtpPort() != null ? c.getSmtpPort() : 25;
        mailSender.setPort(port);
        mailSender.setUsername(c.getSmtpUsername());
        mailSender.setPassword(c.getSmtpPassword());
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(StringUtils.hasText(c.getSmtpUsername())));
        props.put("mail.debug", "false");
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "120000");
        props.put("mail.smtp.writetimeout", "120000");

        boolean ssl = c.getSmtpSslEnabled() != null && c.getSmtpSslEnabled() == 1;
        boolean tls = c.getSmtpTlsEnabled() == null || c.getSmtpTlsEnabled() == 1;
        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.starttls.enable", "false");
        } else {
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", Boolean.toString(tls));
        }
        return mailSender;
    }
}
