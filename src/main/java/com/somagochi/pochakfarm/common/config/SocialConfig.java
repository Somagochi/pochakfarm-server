package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.properties.AppleOAuthProperties;
import com.somagochi.pochakfarm.common.properties.AppleProperties;
import com.somagochi.pochakfarm.common.properties.JwksCacheProperties;
import com.somagochi.pochakfarm.common.properties.KakaoProperties;
import com.somagochi.pochakfarm.common.properties.NaverProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
  KakaoProperties.class,
  NaverProperties.class,
  AppleProperties.class,
  AppleOAuthProperties.class,
  JwksCacheProperties.class
})
public class SocialConfig {}
