package com.somagochi.pochakfarm.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.security.oauth2.login")
public record OAuth2LoginProperties(String successRedirectUri) {}
