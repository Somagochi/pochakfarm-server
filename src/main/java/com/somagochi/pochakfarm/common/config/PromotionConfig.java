package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.properties.PromotionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PromotionProperties.class)
public class PromotionConfig {}
