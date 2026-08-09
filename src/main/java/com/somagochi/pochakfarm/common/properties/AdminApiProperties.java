package com.somagochi.pochakfarm.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public record AdminApiProperties(String apiKey) {}
