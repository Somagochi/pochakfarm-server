package com.somagochi.pochakfarm.common.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.social.oidc.jwks-cache")
public record JwksCacheProperties(Duration ttl, Duration minRefreshInterval) {}
