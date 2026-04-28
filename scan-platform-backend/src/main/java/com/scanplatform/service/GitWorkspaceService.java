package com.scanplatform.service;

import com.scanplatform.entity.ActiveScanRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 克隆或拉取 Git 仓库到本地路径，返回 HEAD commit 与合并日志。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitWorkspaceService {

    private final GitUrlHelper gitUrlHelper;

    @Value("${scan.active-scan.work-base-dir:./active-scan-work}")
    private String workBaseDir;

    /**
     * @return [0]=工作目录绝对路径, [1]=commit hash, [2]=clone/fetch 日志
     */
    public String[] ensureRepo(ActiveScanRepo repo) throws Exception {
        Path base = Path.of(workBaseDir).toAbsolutePath().normalize();
        Files.createDirectories(base);

        Path workDir;
        if (StringUtils.hasText(repo.getLocalClonePath())) {
            workDir = Path.of(repo.getLocalClonePath()).toAbsolutePath().normalize();
        } else {
            workDir = base.resolve("repo-" + repo.getId());
        }
        Files.createDirectories(workDir.getParent() != null ? workDir.getParent() : workDir);

        String authUrl = gitUrlHelper.embedCredentials(repo.getGitUrl(), repo.getGitUsername(), repo.getGitPassword());
        String branch = StringUtils.hasText(repo.getBranch()) ? repo.getBranch() : "main";

        StringBuilder logOut = new StringBuilder();
        if (!Files.isDirectory(workDir.resolve(".git"))) {
            runGit(workDir.getParent(), logOut, "git", "clone", "-b", branch, "--depth", "1", authUrl, workDir.getFileName().toString());
            // clone 在 parent 执行，工作目录为 workDir
        } else {
            runGit(workDir, logOut, "git", "fetch", "origin", branch);
            runGit(workDir, logOut, "git", "checkout", branch);
            runGit(workDir, logOut, "git", "pull", "--ff-only", "origin", branch);
        }

        String commit = readHeadCommit(workDir);
        return new String[]{workDir.toString(), commit, logOut.toString()};
    }

    private void runGit(Path cwd, StringBuilder aggregate, String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(cwd.toFile());
        pb.redirectErrorStream(true);
        log.info("git: {} in {}", String.join(" ", cmd), cwd);
        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                aggregate.append(line).append('\n');
            }
        }
        if (!p.waitFor(30, TimeUnit.MINUTES)) {
            p.destroyForcibly();
            throw new IllegalStateException("git 超时: " + String.join(" ", cmd));
        }
        if (p.exitValue() != 0) {
            throw new IllegalStateException("git 失败 exit=" + p.exitValue() + " cmd=" + String.join(" ", cmd) + "\n" + aggregate);
        }
    }

    private String readHeadCommit(Path repoDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
        pb.directory(repoDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            out = r.readLine();
        }
        p.waitFor(1, TimeUnit.MINUTES);
        return out != null ? out.trim() : "";
    }
}
