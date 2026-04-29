package com.scanplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 代码扫描平台（主动扫描、平台技能、邮件配置等）入口。
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ScanPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScanPlatformApplication.class, args);
    }
}
