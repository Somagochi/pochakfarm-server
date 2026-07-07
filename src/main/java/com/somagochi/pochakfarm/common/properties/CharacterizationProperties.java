package com.somagochi.pochakfarm.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.characterization")
public record CharacterizationProperties(boolean deviceLimitEnabled) {}
