package com.somagochi.pochakfarm.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.develop.admin")
public record DevelopAdminProperties(String username, String password) {}
