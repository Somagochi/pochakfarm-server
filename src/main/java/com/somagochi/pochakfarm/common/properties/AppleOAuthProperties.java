package com.somagochi.pochakfarm.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.security.apple")
public record AppleOAuthProperties(
    String clientId,
    String teamId,
    String keyId,
    String privateKeyPath,
    String redirectUri,
    String tokenUri) {}
