package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.properties.PreRegistrationCryptoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PreRegistrationCryptoProperties.class)
public class PreRegistrationCryptoConfig {}
