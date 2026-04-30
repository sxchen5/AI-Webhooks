package com.scanplatform.service;

import com.scanplatform.dto.CaptchaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单算术/字符图形验证码，内存存储（单机有效；多实例需改为 Redis 等）。
 */
@Service
@Slf4j
public class LoginCaptchaService {

    private static final int TTL_SECONDS = 120;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    public CaptchaResponse newCaptcha() {
        purgeExpired();
        String code = randomCode(4);
        String id = UUID.randomUUID().toString().replace("-", "");
        store.put(id, new CaptchaEntry(code.toLowerCase(), Instant.now().plusSeconds(TTL_SECONDS)));
        String b64;
        try {
            b64 = renderPngBase64(code);
        } catch (IOException e) {
            store.remove(id);
            throw new IllegalStateException("生成验证码失败", e);
        }
        return new CaptchaResponse(id, b64);
    }

    /**
     * @return true 校验通过并已消费该 captchaId
     */
    public boolean verifyAndConsume(String captchaId, String userInput) {
        if (captchaId == null || captchaId.isBlank() || userInput == null) {
            return false;
        }
        CaptchaEntry entry = store.remove(captchaId.trim());
        if (entry == null) {
            return false;
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            return false;
        }
        String normalized = userInput.trim().toLowerCase();
        return normalized.equals(entry.code());
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String renderPngBase64(String code) throws IOException {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(180 + random.nextInt(60), 180 + random.nextInt(60), 190 + random.nextInt(50), 120));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(30 + random.nextInt(80), 50 + random.nextInt(100), 120 + random.nextInt(80)));
            int x = 16 + i * 24 + random.nextInt(4);
            int y = 30 + random.nextInt(6);
            double theta = (random.nextDouble() - 0.5) * 0.35;
            g.rotate(theta, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-theta, x, y);
        }
        for (int i = 0; i < 40; i++) {
            g.setColor(new Color(100 + random.nextInt(100), 100 + random.nextInt(100), 100 + random.nextInt(100)));
            g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
        }
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private record CaptchaEntry(String code, Instant expiresAt) {}
}
