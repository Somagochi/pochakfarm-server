package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.jwt.JwtHelper;
import com.somagochi.pochakfarm.common.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

  @Bean
  JwtHelper jwtHelper(JwtProperties jwtProperties) {
    return new JwtHelper(jwtProperties.secret());
  }
}
