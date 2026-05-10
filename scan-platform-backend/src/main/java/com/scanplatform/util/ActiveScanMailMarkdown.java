package com.scanplatform.util;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Safelist;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * 主动扫描通知邮件正文：摘要行 + 将 execResult（Markdown）转为 HTML。
 * 渲染配置对齐前端 {@code markdown-it}：自动链接、GFM 表格，单换行近似 {@code breaks:true}；渲染后经 Jsoup 白名单清洗。
 */
public final class ActiveScanMailMarkdown {

    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;

    /** 邮件客户端兼容的精简样式（作用域 .scan-md） */
    private static final String EMAIL_SCAN_MD_CSS =
            """
                    .scan-md{font-size:14px;line-height:1.6;color:#24292f;}
                    .scan-md .scan-md--empty{color:#57606a;margin:0;}
                    .scan-md h1,.scan-md h2,.scan-md h3,.scan-md h4,.scan-md h5,.scan-md h6{margin:0.75em 0 0.35em;font-weight:600;line-height:1.25;color:#24292f;}
                    .scan-md h1{font-size:1.35em;}
                    .scan-md h2{font-size:1.2em;}
                    .scan-md h3{font-size:1.05em;}
                    .scan-md p{margin:0.5em 0;}
                    .scan-md ul,.scan-md ol{margin:0.5em 0;padding-left:1.4em;}
                    .scan-md pre{margin:0.6em 0;padding:10px 12px;border-radius:6px;background:#f6f8fa;overflow-x:auto;}
                    .scan-md code{font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,monospace;font-size:0.9em;}
                    .scan-md p code,.scan-md li code{padding:2px 6px;border-radius:4px;background:#f6f8fa;}
                    .scan-md pre code{padding:0;background:transparent;}
                    .scan-md blockquote{margin:0.5em 0;padding:4px 12px;border-left:4px solid #0969da;color:#57606a;}
                    .scan-md table{border-collapse:collapse;width:100%;margin:0.6em 0;}
                    .scan-md th,.scan-md td{border:1px solid #d0d7de;padding:6px 10px;}
                    .scan-md a{color:#0969da;}
                    .scan-md-pre-fallback{margin:12px 0 0;padding:10px;background:#f6f8fa;white-space:pre-wrap;word-break:break-word;font-size:13px;}
                    """;

    static {
        MutableDataSet options = new MutableDataSet();
        options.set(
                Parser.EXTENSIONS,
                List.of(
                        TablesExtension.create(),
                        AutolinkExtension.create()));
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        PARSER = Parser.builder(options).build();
        RENDERER = HtmlRenderer.builder(options).build();
    }

    private ActiveScanMailMarkdown() {}

    public static String buildNotificationEmailHtml(
            String jobName, String repoName, String trigger, String branch, String execResultMarkdown) {
        String j = jobName != null ? jobName : "";
        String r = repoName != null ? repoName : "";
        String t = trigger != null ? trigger : "";
        String b = branch != null ? branch : "";
        String header =
                "<div><b>任务</b>：" + HtmlUtils.htmlEscape(j) + "</div>"
                        + "<div><b>仓库</b>：" + HtmlUtils.htmlEscape(r) + "</div>"
                        + "<div><b>触发</b>：" + HtmlUtils.htmlEscape(t) + "</div>"
                        + "<div><b>分支</b>：" + HtmlUtils.htmlEscape(b) + "</div>";
        String fragment = renderFragment(execResultMarkdown);
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>"
                + EMAIL_SCAN_MD_CSS
                + "</style></head><body style=\"margin:12px;font-family:system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;\">"
                + header
                + "<div class=\"scan-md\" style=\"margin-top:12px;\">"
                + fragment
                + "</div></body></html>";
    }

    private static String renderFragment(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "<p class=\"scan-md--empty\">（无输出）</p>";
        }
        try {
            String raw = RENDERER.render(PARSER.parse(markdown));
            return sanitizeMarkdownHtml(raw);
        } catch (Exception e) {
            return "<pre class=\"scan-md-pre-fallback\">" + HtmlUtils.htmlEscape(markdown) + "</pre>";
        }
    }

    /** 与 Markdown 输出匹配的宽松白名单，去掉脚本/事件等危险内容 */
    private static String sanitizeMarkdownHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.clean(html, "", Safelist.relaxed(), new OutputSettings().prettyPrint(false));
    }
}
