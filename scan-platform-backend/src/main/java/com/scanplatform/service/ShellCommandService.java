package com.scanplatform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 在指定工作目录下异步执行 Shell 命令（通过 bash -c），收集标准输出与错误输出。
 */
@Service
@Slf4j
public class ShellCommandService {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofHours(2);

    /**
     * @param workingDir 工作目录，可为空则使用 JVM 当前目录
     * @param extraEnv   追加到子进程环境变量（如 WEBHOOK_*），便于 Shell 脚本与 Cursor CLI 使用
     * @return 合并后的控制台输出与退出码
     */
    public ShellResult execute(String command, Path workingDir, Map<String, String> extraEnv) throws Exception {
        Path dir = workingDir != null && Files.isDirectory(workingDir) ? workingDir : Path.of("").toAbsolutePath();
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
        pb.directory(dir.toFile());
        if (extraEnv != null && !extraEnv.isEmpty()) {
            pb.environment().putAll(extraEnv);
        }
        log.debug("执行 Shell: dir={} envKeys={}", dir, extraEnv != null ? extraEnv.keySet() : "none");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        boolean finished = process.waitFor(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.error("Shell 执行超时已终止: dir={}", dir);
            throw new IllegalStateException("命令执行超时，已终止进程");
        }
        int code = process.exitValue();
        if (code != 0) {
            log.warn("Shell 退出码非零: exitCode={} dir={}", code, dir);
        }
        return new ShellResult(out.toString(), code);
    }

    /** 不注入额外环境变量时的便捷重载。 */
    public ShellResult execute(String command, Path workingDir) throws Exception {
        return execute(command, workingDir, Map.of());
    }

    public record ShellResult(String output, int exitCode) {
    }
}
