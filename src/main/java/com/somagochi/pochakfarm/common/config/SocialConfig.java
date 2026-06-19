package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.properties.KakaoProperties;
import com.somagochi.pochakfarm.common.properties.NaverProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({KakaoProperties.class, NaverProperties.class})
public class SocialConfig {}
