package com.somagochi.pochakfarm.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.social.kakao")
public record KakaoProperties(String baseUrl, String userInfoPath) {}
