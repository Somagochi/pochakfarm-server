package com.somagochi.pochakfarm.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.solapi")
public record SolapiProperties(String baseUrl, String apiKey, String apiSecret, String from) {}
