package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.properties.CaptureProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CaptureProperties.class)
public class CaptureConfig {}
