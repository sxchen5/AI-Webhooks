package com.scanplatform.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanplatform.dto.PlatformSkillFileDto;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 平台技能多文件载荷：JSON 存库，与旧单列 {@code skill_body}（仅 SKILL.md）兼容。
 */
public final class PlatformSkillPayloadCodec {

    public static final String PRIMARY_FILE = "SKILL.md";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<PlatformSkillFileDto>> LIST_TYPE = new TypeReference<>() {};
    /** Windows 非法文件名字符及路径分隔符 */
    private static final Pattern INVALID_IN_SEGMENT = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final int MAX_FILES = 120;
    private static final int MAX_PATH_LEN = 240;
    private static final int MAX_BYTES_PER_FILE = 512 * 1024;
    private static final int MAX_TOTAL_BYTES = 8 * 1024 * 1024;

    private PlatformSkillPayloadCodec() {}

    public static List<PlatformSkillFileDto> decode(String json, String legacySkillBody) {
        if (StringUtils.hasText(json)) {
            try {
                List<PlatformSkillFileDto> list = MAPPER.readValue(json.trim(), LIST_TYPE);
                if (list != null && !list.isEmpty()) {
                    return normalize(list);
                }
            } catch (Exception ignored) {
                // fall through to legacy
            }
        }
        if (StringUtils.hasText(legacySkillBody)) {
            return List.of(new PlatformSkillFileDto(PRIMARY_FILE, legacySkillBody));
        }
        return List.of();
    }

    public static String encode(List<PlatformSkillFileDto> files) {
        try {
            return MAPPER.writeValueAsString(normalize(files));
        } catch (Exception e) {
            throw new IllegalStateException("序列化技能文件失败: " + e.getMessage(), e);
        }
    }

    public static List<PlatformSkillFileDto> normalize(List<PlatformSkillFileDto> input) {
        Map<String, String> byPath = new LinkedHashMap<>();
        for (PlatformSkillFileDto f : input) {
            if (f == null || !StringUtils.hasText(f.getPath())) {
                continue;
            }
            String p = normalizePath(f.getPath());
            if (PRIMARY_FILE.equalsIgnoreCase(p)) {
                p = PRIMARY_FILE;
            }
            String c = f.getContent() != null ? f.getContent() : "";
            byPath.put(p, c);
        }
        List<PlatformSkillFileDto> out = new ArrayList<>();
        byPath.forEach((p, c) -> out.add(new PlatformSkillFileDto(p, c)));
        out.sort(
                Comparator.comparing((PlatformSkillFileDto f) -> !PRIMARY_FILE.equals(f.getPath()))
                        .thenComparing(PlatformSkillFileDto::getPath, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public static void validate(List<PlatformSkillFileDto> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个技能文件");
        }
        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException("文件数量不能超过 " + MAX_FILES);
        }
        boolean hasPrimary = false;
        long total = 0;
        for (PlatformSkillFileDto f : files) {
            String p = normalizePath(f.getPath());
            if (PRIMARY_FILE.equalsIgnoreCase(p)) {
                p = PRIMARY_FILE;
            }
            if (!StringUtils.hasText(p)) {
                throw new IllegalArgumentException("存在空路径文件");
            }
            if (p.length() > MAX_PATH_LEN) {
                throw new IllegalArgumentException("路径过长: " + p);
            }
            validatePathSegments(p);
            String content = f.getContent() != null ? f.getContent() : "";
            if (PRIMARY_FILE.equals(p) && !StringUtils.hasText(content)) {
                throw new IllegalArgumentException(PRIMARY_FILE + " 内容不能为空");
            }
            byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (bytes.length > MAX_BYTES_PER_FILE) {
                throw new IllegalArgumentException("单文件过大（上限 " + (MAX_BYTES_PER_FILE / 1024) + "KB）: " + p);
            }
            total += bytes.length;
            if (PRIMARY_FILE.equals(p)) {
                hasPrimary = true;
            }
        }
        if (!hasPrimary) {
            throw new IllegalArgumentException("必须包含根路径文件 " + PRIMARY_FILE);
        }
        if (total > MAX_TOTAL_BYTES) {
            throw new IllegalArgumentException("全部文件总大小不能超过 " + (MAX_TOTAL_BYTES / (1024 * 1024)) + "MB");
        }
    }

    private static void validatePathSegments(String path) {
        if (path.startsWith("/") || path.contains("..")) {
            throw new IllegalArgumentException("非法路径: " + path);
        }
        for (String seg : path.split("/")) {
            if (seg.isEmpty()) {
                throw new IllegalArgumentException("非法路径(空段): " + path);
            }
            if (".".equals(seg) || "..".equals(seg)) {
                throw new IllegalArgumentException("非法路径: " + path);
            }
            if (seg.length() > 120) {
                throw new IllegalArgumentException("路径段过长: " + path);
            }
            if (INVALID_IN_SEGMENT.matcher(seg).find()) {
                throw new IllegalArgumentException("路径含非法字符: " + path);
            }
        }
    }

    public static String normalizePath(String raw) {
        String p = raw.trim().replace('\\', '/');
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    public static String extractPrimaryContent(List<PlatformSkillFileDto> files) {
        for (PlatformSkillFileDto f : files) {
            if (PRIMARY_FILE.equals(f.getPath())) {
                return f.getContent() != null ? f.getContent() : "";
            }
        }
        return "";
    }
}
