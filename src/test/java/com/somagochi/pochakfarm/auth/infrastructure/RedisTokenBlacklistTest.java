package com.somagochi.pochakfarm.auth.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisTokenBlacklistTest {

  private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

  @SuppressWarnings("unchecked")
  private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

  private final RedisTokenBlacklist tokenBlacklist = new RedisTokenBlacklist(redisTemplate);

  @Test
  void registersTokenWithRemainingTtl() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);

    tokenBlacklist.register("jti-1", Duration.ofMinutes(5));

    verify(valueOperations).set("blacklist:jti-1", "logout", Duration.ofMinutes(5));
  }

  @Test
  void doesNotRegisterWhenTtlIsNotPositive() {
    tokenBlacklist.register("jti-1", Duration.ZERO);

    verify(redisTemplate, never()).opsForValue();
  }

  @Test
  void rejectsBlankTokenIdOnRegister() {
    assertThrows(
        IllegalArgumentException.class, () -> tokenBlacklist.register("  ", Duration.ofMinutes(5)));
  }

  @Test
  void returnsTrueWhenKeyExists() {
    given(redisTemplate.hasKey("blacklist:jti-1")).willReturn(true);

    assertTrue(tokenBlacklist.isBlacklisted("jti-1"));
  }

  @Test
  void returnsFalseWhenKeyAbsent() {
    given(redisTemplate.hasKey("blacklist:jti-1")).willReturn(false);

    assertFalse(tokenBlacklist.isBlacklisted("jti-1"));
  }

  @Test
  void returnsFalseForBlankTokenIdWithoutQueryingRedis() {
    assertFalse(tokenBlacklist.isBlacklisted(null));

    verify(redisTemplate, never()).hasKey("blacklist:null");
  }
}
