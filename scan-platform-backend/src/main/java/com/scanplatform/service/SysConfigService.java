package com.scanplatform.service;

import com.scanplatform.dto.SysConfigDto;
import com.scanplatform.entity.SysConfig;
import com.scanplatform.repository.SysConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 全局配置：单表通常仅一行，取第一条进行读写。
 */
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigRepository configRepository;

    @Transactional(readOnly = true)
    public SysConfigDto get() {
        SysConfig c = configRepository.findAll().stream().findFirst().orElseGet(this::createDefault);
        return toDto(c, false);
    }

    @Transactional(readOnly = true)
    public SysConfig getEntity() {
        return configRepository.findAll().stream().findFirst().orElseGet(this::createDefault);
    }

    @Transactional
    public SysConfigDto save(SysConfigDto dto) {
        SysConfig c = configRepository.findAll().stream().findFirst().orElseGet(this::createDefault);
        c.setWebhookToken(dto.getWebhookToken());
        c.setGitlabAllowIps(dto.getGitlabAllowIps());
        c.setSmtpHost(dto.getSmtpHost());
        c.setSmtpPort(dto.getSmtpPort());
        c.setSmtpUsername(dto.getSmtpUsername());
        if (StringUtils.hasText(dto.getSmtpPassword())) {
            c.setSmtpPassword(dto.getSmtpPassword());
        }
        if (StringUtils.hasText(dto.getEmailTitlePrefix())) {
            c.setEmailTitlePrefix(dto.getEmailTitlePrefix());
        }
        c = configRepository.save(c);
        return toDto(c, true);
    }

    private SysConfig createDefault() {
        SysConfig c = new SysConfig();
        c.setEmailTitlePrefix("【代码扫描通知】");
        return configRepository.save(c);
    }

    private SysConfigDto toDto(SysConfig c, boolean maskPassword) {
        SysConfigDto d = new SysConfigDto();
        d.setId(c.getId());
        d.setWebhookToken(c.getWebhookToken());
        d.setGitlabAllowIps(c.getGitlabAllowIps());
        d.setSmtpHost(c.getSmtpHost());
        d.setSmtpPort(c.getSmtpPort());
        d.setSmtpUsername(c.getSmtpUsername());
        d.setSmtpPassword(maskPassword && StringUtils.hasText(c.getSmtpPassword()) ? "******" : null);
        d.setEmailTitlePrefix(c.getEmailTitlePrefix());
        return d;
    }
}
