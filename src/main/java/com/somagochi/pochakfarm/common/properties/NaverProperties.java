package com.somagochi.pochakfarm.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.social.naver")
public record NaverProperties(String baseUrl, String userInfoPath) {}
