package com.scanplatform.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Vue Router history 模式：非 API、非静态资源路径回退到 index.html。
 * 顺序最低，避免拦截已有 Controller。
 */
@Controller
@Order(Ordered.LOWEST_PRECEDENCE)
public class SpaForwardController {

    @GetMapping(value = {
            "/",
            "/login",
            "/projects",
            "/sys-config",
            "/scan-logs",
            "/active-scan/repos",
            "/active-scan/jobs",
            "/active-scan/logs"
    })
    public String forwardSpa() {
        return "forward:/index.html";
    }
}
