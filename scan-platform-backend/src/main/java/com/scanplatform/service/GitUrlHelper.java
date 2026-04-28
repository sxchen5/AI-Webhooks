package com.scanplatform.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 为 HTTP(S) Git URL 嵌入用户名密码，供 git clone 非交互使用。
 */
@Component
public class GitUrlHelper {

    /**
     * @return 可传给 git 的 URL；非 http(s) 或无需凭据时返回原 URL
     */
    public String embedCredentials(String gitUrl, String username, String password) {
        if (!StringUtils.hasText(gitUrl)) {
            return gitUrl;
        }
        String lower = gitUrl.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return gitUrl;
        }
        if (!StringUtils.hasText(username)) {
            return gitUrl;
        }
        try {
            URI uri = URI.create(gitUrl);
            String userInfo = URLEncoder.encode(username, StandardCharsets.UTF_8);
            if (StringUtils.hasText(password)) {
                userInfo += ":" + URLEncoder.encode(password, StandardCharsets.UTF_8);
            }
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            String hostPart = host + (port > 0 ? ":" + port : "");
            String path = uri.getRawPath() != null ? uri.getRawPath() : "";
            String query = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";
            String fragment = uri.getRawFragment() != null ? "#" + uri.getRawFragment() : "";
            return scheme + "://" + userInfo + "@" + hostPart + path + query + fragment;
        } catch (Exception e) {
            return gitUrl;
        }
    }
}
