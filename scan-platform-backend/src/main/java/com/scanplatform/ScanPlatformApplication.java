package com.scanplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * GitLab WebHook 触发的代码扫描平台入口。
 */
@SpringBootApplication
@EnableAsync
public class ScanPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScanPlatformApplication.class, args);
    }
}
