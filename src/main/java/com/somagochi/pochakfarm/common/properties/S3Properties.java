package com.somagochi.pochakfarm.common.properties;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.s3")
public record S3Properties(
    String bucket,
    String region,
    String accessKey,
    String secretKey,
    String endpoint,
    Duration presignExpiration,
    long maxFileSize,
    List<String> allowedContentTypes) {}
