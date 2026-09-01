package com.somagochi.pochakfarm.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.SystemPropertyUtils;

@SpringBootTest
@TestPropertySource(
    properties = {"spring.data.redis.timeout=2s", "spring.data.redis.connect-timeout=3s"})
@DisplayName("Redis 클라이언트 타임아웃 설정")
class RedisTimeoutConfigTest {

  private static final String APPLICATION_YML = "src/main/resources/application.yml";
  private static final Duration LETTUCE_DEFAULT_COMMAND_TIMEOUT = Duration.ofSeconds(60);
  private static final Duration MAX_ALLOWED_TIMEOUT = Duration.ofSeconds(5);

  @Autowired private LettuceConnectionFactory lettuceConnectionFactory;

  @Test
  @DisplayName("spring.data.redis.timeout 이 Lettuce 명령 타임아웃으로 적용된다")
  void appliesConfiguredCommandTimeout() {
    Duration commandTimeout = lettuceConnectionFactory.getClientConfiguration().getCommandTimeout();

    assertEquals(Duration.ofSeconds(2), commandTimeout);
    assertTrue(commandTimeout.compareTo(LETTUCE_DEFAULT_COMMAND_TIMEOUT) < 0);
  }

  @Test
  @DisplayName("spring.data.redis.connect-timeout 이 Lettuce 소켓 커넥션 타임아웃으로 적용된다")
  void appliesConfiguredConnectTimeout() {
    Duration connectTimeout =
        lettuceConnectionFactory
            .getClientConfiguration()
            .getClientOptions()
            .orElseThrow()
            .getSocketOptions()
            .getConnectTimeout();

    assertEquals(Duration.ofSeconds(3), connectTimeout);
  }

  @Test
  @DisplayName("운영 application.yml 의 Redis 타임아웃 기본값이 60초보다 훨씬 짧다")
  void keepsShortRedisTimeoutDefaultsInApplicationYml() {
    assertTrue(
        applicationYmlDuration("spring.data.redis.timeout").compareTo(MAX_ALLOWED_TIMEOUT) <= 0,
        "Redis 명령 타임아웃 기본값이 너무 깁니다");
    assertTrue(
        applicationYmlDuration("spring.data.redis.connect-timeout").compareTo(MAX_ALLOWED_TIMEOUT)
            <= 0,
        "Redis 커넥션 타임아웃 기본값이 너무 깁니다");
  }

  private Duration applicationYmlDuration(String key) {
    return DurationStyle.detectAndParse(
        SystemPropertyUtils.resolvePlaceholders(applicationYmlValue(key)));
  }

  private String applicationYmlValue(String key) {
    for (PropertySource<?> source : applicationYmlSources()) {
      Object value = source.getProperty(key);
      if (value != null) {
        return value.toString();
      }
    }
    throw new IllegalStateException("application.yml 에 " + key + " 설정이 없습니다");
  }

  private List<PropertySource<?>> applicationYmlSources() {
    try {
      return new YamlPropertySourceLoader()
          .load("application", new FileSystemResource(APPLICATION_YML));
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
