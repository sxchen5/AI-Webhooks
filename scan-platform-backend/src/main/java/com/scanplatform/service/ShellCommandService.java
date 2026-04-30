package com.scanplatform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 在指定工作目录下执行 Shell 命令并收集输出。
 * Windows 默认使用 {@code cmd.exe /c}；类 Unix 使用 {@code /bin/bash -c}；可通过 {@code scan.shell.executable} 指定 Git Bash 等。
 */
@Service
@Slf4j
public class ShellCommandService {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofHours(2);

    /** 非空则作为可执行文件，参数为 {@code -c} 与命令行（须支持 -c，如 bash、sh） */
    @Value("${scan.shell.executable:}")
    private String shellExecutable;

    /**
     * @param workingDir 工作目录，可为空则使用 JVM 当前目录
     * @param extraEnv   追加到子进程环境变量（如 WEBHOOK_*），便于 Shell 脚本与 Cursor CLI 使用
     * @return 合并后的控制台输出与退出码
     */
    public ShellResult execute(String command, Path workingDir, Map<String, String> extraEnv) throws Exception {
        Path dir = workingDir != null && Files.isDirectory(workingDir) ? workingDir : Path.of("").toAbsolutePath();
        List<String> cmdLine = buildShellCommandLine(command);
        ProcessBuilder pb = new ProcessBuilder(cmdLine);
        pb.directory(dir.toFile());
        if (extraEnv != null && !extraEnv.isEmpty()) {
            pb.environment().putAll(extraEnv);
        }
        log.debug("执行 Shell: {} dir={} envKeys={}", cmdLine.get(0), dir, extraEnv != null ? extraEnv.keySet() : "none");
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

    /**
     * 流式读取标准输出（stderr 已合并），每行调用一次 consumer；进程结束后再返回退出码。
     */
    public int executeStreaming(String command, Path workingDir, Map<String, String> extraEnv,
                                Consumer<String> lineConsumer) throws Exception {
        Path dir = workingDir != null && Files.isDirectory(workingDir) ? workingDir : Path.of("").toAbsolutePath();
        List<String> cmdLine = buildShellCommandLine(command);
        ProcessBuilder pb = new ProcessBuilder(cmdLine);
        pb.directory(dir.toFile());
        if (extraEnv != null && !extraEnv.isEmpty()) {
            pb.environment().putAll(extraEnv);
        }
        log.debug("流式执行 Shell: {} dir={}", cmdLine.get(0), dir);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineConsumer.accept(line);
            }
        }
        boolean finished = process.waitFor(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("命令执行超时，已终止进程");
        }
        return process.exitValue();
    }

    private List<String> buildShellCommandLine(String command) {
        List<String> list = new ArrayList<>();
        if (StringUtils.hasText(shellExecutable)) {
            list.add(shellExecutable.trim());
            list.add("-c");
            list.add(command);
            return list;
        }
        if (isWindows()) {
            list.add("cmd.exe");
            list.add("/c");
            list.add(command);
            return list;
        }
        list.add("/bin/bash");
        list.add("-c");
        list.add(command);
        return list;
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("win");
    }

    /** 不注入额外环境变量时的便捷重载。 */
    public ShellResult execute(String command, Path workingDir) throws Exception {
        return execute(command, workingDir, Map.of());
    }

    public record ShellResult(String output, int exitCode) {
    }
}
