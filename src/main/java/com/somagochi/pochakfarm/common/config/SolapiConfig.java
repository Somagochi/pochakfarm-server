package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.properties.SolapiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SolapiProperties.class)
public class SolapiConfig {}
