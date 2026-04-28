package com.scanplatform.service;

import com.scanplatform.entity.SysConfig;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * 根据 sys_config 动态 SMTP 发送告警邮件（失败时调用）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertMailService {

    /**
     * @return true 表示已尝试发送且 SMTP 返回成功
     */
    public boolean sendFailureAlert(SysConfig config, String toAddresses, String subject, String text) {
        if (!StringUtils.hasText(toAddresses)) {
            log.warn("未配置告警邮箱，跳过邮件发送");
            return false;
        }
        if (!StringUtils.hasText(config.getSmtpHost()) || config.getSmtpPort() == null) {
            log.warn("未配置 SMTP 主机或端口，跳过邮件发送");
            return false;
        }
        JavaMailSenderImpl sender = buildSender(config);
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String from = StringUtils.hasText(config.getSmtpUsername()) ? config.getSmtpUsername() : "noreply@localhost";
            helper.setFrom(from);
            helper.setTo(toAddresses.split(","));
            helper.setSubject(subject);
            helper.setText(text, false);
            sender.send(message);
            return true;
        } catch (Exception e) {
            log.error("发送告警邮件失败: {}", e.getMessage());
            return false;
        }
    }

    private JavaMailSenderImpl buildSender(SysConfig c) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(c.getSmtpHost());
        mailSender.setPort(c.getSmtpPort());
        mailSender.setUsername(c.getSmtpUsername());
        mailSender.setPassword(c.getSmtpPassword());
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", StringUtils.hasText(c.getSmtpUsername()));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");
        return mailSender;
    }
}
