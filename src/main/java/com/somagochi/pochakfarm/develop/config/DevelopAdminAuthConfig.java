package com.somagochi.pochakfarm.develop.config;

import com.somagochi.pochakfarm.common.properties.DevelopAdminProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.util.StringUtils;

@Configuration
@Profile({"local", "dev"})
@EnableConfigurationProperties(DevelopAdminProperties.class)
public class DevelopAdminAuthConfig {

  @Bean
  UserDetailsService developAdminUserDetailsService(
      DevelopAdminProperties properties, PasswordEncoder passwordEncoder) {
    if (!StringUtils.hasText(properties.username())
        || !StringUtils.hasText(properties.password())) {
      throw new IllegalStateException("secret.develop.admin.username/password 설정이 필요합니다");
    }
    return new InMemoryUserDetailsManager(
        User.withUsername(properties.username())
            .password(passwordEncoder.encode(properties.password()))
            .roles("DEV_ADMIN")
            .build());
  }
}
