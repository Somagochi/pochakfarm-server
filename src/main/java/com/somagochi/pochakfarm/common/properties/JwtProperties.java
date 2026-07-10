package com.somagochi.pochakfarm.common.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret, Duration accessTokenExpiration, Duration refreshTokenExpiration) {}
