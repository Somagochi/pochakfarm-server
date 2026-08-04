package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.properties.AdminApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdminApiProperties.class)
public class AdminApiConfig {}
