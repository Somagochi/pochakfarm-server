package com.somagochi.pochakfarm.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.promotion")
public record PromotionProperties(String baseUrl) {}
