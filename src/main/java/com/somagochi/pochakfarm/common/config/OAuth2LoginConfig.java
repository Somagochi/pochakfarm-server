package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.properties.OAuth2LoginProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OAuth2LoginProperties.class)
public class OAuth2LoginConfig {}
