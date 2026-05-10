package com.scanplatform.service;

import com.scanplatform.dto.PlatformSkillFileDto;
import com.scanplatform.entity.PlatformSkill;
import com.scanplatform.repository.PlatformSkillRepository;
import com.scanplatform.util.PlatformSkillPayloadCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * 若平台库中存在同名启用技能，则写入工作区 {@code .cursor/skills/<name>/} 下全部配置的文件，覆盖仓库内同名目录。
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
                List<PlatformSkillFileDto> files =
                        PlatformSkillPayloadCodec.decode(skill.getSkillFilesJson(), skill.getSkillBody());
                if (files.isEmpty()) {
                    log.warn("平台技能无文件内容，跳过写入: {}", skillSlug);
                    return;
                }
                if (Files.exists(dir)) {
                    FileSystemUtils.deleteRecursively(dir);
                }
                Files.createDirectories(dir);
                for (PlatformSkillFileDto f : files) {
                    Path rel = Path.of(f.getPath()).normalize();
                    Path target = dir.resolve(rel).normalize();
                    if (!target.startsWith(dir)) {
                        throw new IllegalStateException("非法技能文件路径: " + f.getPath());
                    }
                    Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.writeString(
                            target,
                            f.getContent() != null ? f.getContent() : "",
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE);
                }
                log.info("已写入平台技能 {} 个文件: {} -> {}", files.size(), skill.getSkillName(), dir);
            } catch (Exception e) {
                log.error("写入平台技能失败: {}", skillSlug, e);
                throw new IllegalStateException("写入平台技能失败: " + e.getMessage(), e);
            }
        }, () -> log.debug("未配置平台技能，使用仓库内 .cursor/skills: {}", skillSlug));
    }
}
