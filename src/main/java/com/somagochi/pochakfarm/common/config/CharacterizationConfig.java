package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.properties.CharacterizationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CharacterizationProperties.class)
public class CharacterizationConfig {}
