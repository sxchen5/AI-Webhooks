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
 * 根据 sys_config 动态 SMTP 发送告警邮件（失败时调用）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertMailService {

    /**
     * @return true 表示已尝试发送且 SMTP 返回成功
     */
    /** 通用发送（主动扫描成功/失败通知等） */
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
                    "SMTP 发送邮件: host={} port={} from={} toCount={} subject={} bodyChars={}",
                    config.getSmtpHost(),
                    config.getSmtpPort(),
                    from,
                    to.length,
                    subject,
                    text != null ? text.length() : 0);
            sender.send(message);
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
