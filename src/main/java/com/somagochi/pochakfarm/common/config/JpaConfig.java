package com.somagochi.pochakfarm.common.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {

  @Bean
  public DateTimeProvider auditingDateTimeProvider() {
    return () -> Optional.of(Instant.now().truncatedTo(ChronoUnit.MICROS));
  }
}
