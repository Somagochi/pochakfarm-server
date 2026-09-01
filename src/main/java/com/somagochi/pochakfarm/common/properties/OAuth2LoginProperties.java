package com.somagochi.pochakfarm.common.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "spring.security.oauth2.login")
public record OAuth2LoginProperties(
    String successRedirectUri, @DefaultValue("PT3M") Duration authorizationRequestTtl) {}
