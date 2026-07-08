package com.somagochi.pochakfarm.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pre-registration.crypto")
public record PreRegistrationCryptoProperties(String encryptionKey, String hashKey) {}
