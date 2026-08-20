package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.properties.BattleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BattleProperties.class)
public class BattleConfig {}
