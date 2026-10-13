package com.campushub.modules.ping.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "app.ping.enabled", havingValue = "true", matchIfMissing = true)
public class SelfPingService {

    private static final Logger log = LoggerFactory.getLogger(SelfPingService.class);

    private final HttpClient httpClient;

    @Value("${app.ping.url:}")
    private String pingUrl;

    @Value("${server.port:8080}")
    private String serverPort;

    public SelfPingService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Tự động ping mỗi 5 phút (300,000ms), bắt đầu sau khi ứng dụng chạy được 1 phút (60,000ms).
     * Render Free Tier sẽ đưa server vào trạng thái ngủ nếu không có request trong vòng 15 phút.
     * Ping mỗi 5 phút sẽ đảm bảo server luôn thức 24/7.
     */
    @Scheduled(fixedRateString = "${app.ping.interval-ms:300000}", initialDelay = 60000)
    public void pingServer() {
        String targetUrl = resolvePingUrl();

        if (targetUrl == null || targetUrl.isBlank()) {
            log.debug("Self-ping bỏ qua: Chưa cấu hình SELF_PING_URL hoặc RENDER_EXTERNAL_URL.");
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "CampusHub-SelfPing/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Self-ping thành công tới {} [HTTP {}]", targetUrl, response.statusCode());
            } else {
                log.warn("Self-ping nhận mã phản hồi bất thường: {} từ {}", response.statusCode(), targetUrl);
            }
        } catch (Exception e) {
            log.warn("Self-ping thất bại tới {}: {}", targetUrl, e.getMessage());
        }
    }

    private String resolvePingUrl() {
        if (pingUrl != null && !pingUrl.isBlank()) {
            String clean = pingUrl.trim();
            if (clean.endsWith("/")) {
                clean = clean.substring(0, clean.length() - 1);
            }
            if (!clean.endsWith("/ping") && !clean.endsWith("/health")) {
                clean = clean + "/api/v1/ping";
            }
            return clean;
        }

        return "http://localhost:" + serverPort + "/api/v1/ping";
    }
}
