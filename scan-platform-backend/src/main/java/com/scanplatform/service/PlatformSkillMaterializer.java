package com.scanplatform.service;

import com.scanplatform.entity.PlatformSkill;
import com.scanplatform.repository.PlatformSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 若平台库中存在同名启用技能，则写入工作区 {@code .cursor/skills/<name>/SKILL.md}，覆盖仓库内同名目录（Cursor 加载优先级以本次扫描目录为准）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformSkillMaterializer {

    private final PlatformSkillRepository repository;

    /**
     * @param workRoot   被扫描代码根目录（绝对路径）
     * @param skillSlug  已通过校验的技能目录名
     */
    public void materializeIfPresent(Path workRoot, String skillSlug) {
        if (!StringUtils.hasText(skillSlug)) {
            return;
        }
        repository.findBySkillNameAndStatus(skillSlug.trim(), 1).ifPresentOrElse(skill -> {
            try {
                Path dir = workRoot.resolve(".cursor").resolve("skills").resolve(skillSlug.trim()).normalize();
                if (!dir.startsWith(workRoot.normalize())) {
                    throw new IllegalStateException("非法技能路径");
                }
                Files.createDirectories(dir);
                Path file = dir.resolve("SKILL.md");
                Files.writeString(file, skill.getSkillBody(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                log.info("已写入平台技能覆盖: {} -> {}", skill.getSkillName(), file);
            } catch (Exception e) {
                log.error("写入平台技能失败: {}", skillSlug, e);
                throw new IllegalStateException("写入平台技能失败: " + e.getMessage(), e);
            }
        }, () -> log.debug("未配置平台技能，使用仓库内 .cursor/skills: {}", skillSlug));
    }
}
