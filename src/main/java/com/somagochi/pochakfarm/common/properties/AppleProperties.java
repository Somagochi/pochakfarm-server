package com.somagochi.pochakfarm.common.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.social.apple")
public record AppleProperties(String issuer, List<String> audiences, String jwksUri) {}
