package com.scanplatform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 计算 Spring 6 段式 Cron 的下次触发时间（秒 分 时 日 月 周）。
 */
@Component
@Slf4j
public class ActiveScanCronHelper {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    public LocalDateTime nextFireTime(String cronExpression, LocalDateTime after) {
        if (!StringUtils.hasText(cronExpression)) {
            return null;
        }
        try {
            CronExpression cron = CronExpression.parse(cronExpression.trim());
            ZonedDateTime base = after.atZone(ZONE);
            ZonedDateTime next = cron.next(base);
            return next != null ? next.toLocalDateTime() : null;
        } catch (Exception e) {
            log.warn("Cron 表达式无效: {}", cronExpression, e);
            return null;
        }
    }
}
