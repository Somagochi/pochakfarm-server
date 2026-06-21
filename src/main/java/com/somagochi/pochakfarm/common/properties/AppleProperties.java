package com.somagochi.pochakfarm.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.social.apple")
public record AppleProperties(String issuer, String audience, String jwksUri) {}
