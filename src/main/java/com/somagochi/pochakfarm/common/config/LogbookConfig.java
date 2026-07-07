package com.somagochi.pochakfarm.common.config;

import static org.zalando.logbook.json.JsonBodyFilters.replaceJsonStringProperty;

import com.somagochi.pochakfarm.common.logging.CorrelationIdFilter;
import java.util.Set;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.CorrelationId;
import org.zalando.logbook.Sink;
import org.zalando.logbook.json.JsonHttpLogFormatter;
import org.zalando.logbook.logstash.LogstashLogbackSink;

@Configuration
public class LogbookConfig {

  private static final Set<String> SENSITIVE_FIELDS =
      Set.of("accessToken", "refreshToken", "token", "password");

  @Bean
  BodyFilter sensitiveBodyFilter() {
    return replaceJsonStringProperty(SENSITIVE_FIELDS::contains, "***");
  }

  @Bean
  Sink logstashSink() {
    return new LogstashLogbackSink(new JsonHttpLogFormatter());
  }

  @Bean
  CorrelationId correlationId() {
    return request -> {
      String id = MDC.get(CorrelationIdFilter.CORRELATION_ID);
      return id != null ? id : UUID.randomUUID().toString();
    };
  }
}
