package com.campushub.infrastructure.ocr.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Spring configuration providing RestClient and connection factory for OCR service integration.
 */
@Configuration
@EnableConfigurationProperties(OcrProperties.class)
public class OcrClientConfig {

    @Bean
    public RestClient.Builder ocrRestClientBuilder(OcrProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory);
    }

    @Bean
    public RestClient ocrRestClient(RestClient.Builder ocrRestClientBuilder) {
        return ocrRestClientBuilder.build();
    }
}
