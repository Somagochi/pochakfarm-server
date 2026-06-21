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

class RedisRefreshTokenWhitelistTest {

  private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

  @SuppressWarnings("unchecked")
  private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

  private final RedisRefreshTokenWhitelist refreshTokenWhitelist =
      new RedisRefreshTokenWhitelist(redisTemplate);

  @Test
  void registersTokenWithRemainingTtl() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);

    refreshTokenWhitelist.register("jti-1", Duration.ofDays(14));

    verify(valueOperations).set("whitelist:refresh:jti-1", "valid", Duration.ofDays(14));
  }

  @Test
  void doesNotRegisterWhenTtlIsNotPositive() {
    refreshTokenWhitelist.register("jti-1", Duration.ZERO);

    verify(redisTemplate, never()).opsForValue();
  }

  @Test
  void rejectsBlankTokenIdOnRegister() {
    assertThrows(
        IllegalArgumentException.class,
        () -> refreshTokenWhitelist.register("  ", Duration.ofDays(14)));
  }

  @Test
  void returnsTrueWhenKeyExists() {
    given(redisTemplate.hasKey("whitelist:refresh:jti-1")).willReturn(true);

    assertTrue(refreshTokenWhitelist.contains("jti-1"));
  }

  @Test
  void returnsFalseWhenKeyAbsent() {
    given(redisTemplate.hasKey("whitelist:refresh:jti-1")).willReturn(false);

    assertFalse(refreshTokenWhitelist.contains("jti-1"));
  }

  @Test
  void removesKey() {
    refreshTokenWhitelist.remove("jti-1");

    verify(redisTemplate).delete("whitelist:refresh:jti-1");
  }

  @Test
  void doesNotQueryRedisForBlankTokenId() {
    assertFalse(refreshTokenWhitelist.contains(null));

    verify(redisTemplate, never()).hasKey("whitelist:refresh:null");
  }
}
