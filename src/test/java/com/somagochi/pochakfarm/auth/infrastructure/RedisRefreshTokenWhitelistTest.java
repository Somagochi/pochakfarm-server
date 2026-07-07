package com.somagochi.pochakfarm.auth.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

class RedisRefreshTokenWhitelistTest {

  private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

  @SuppressWarnings("unchecked")
  private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

  private final RedisRefreshTokenWhitelist refreshTokenWhitelist =
      new RedisRefreshTokenWhitelist(redisTemplate);

  @Test
  void registersTokenWithRemainingTtl() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);

    refreshTokenWhitelist.register("user-1", "jti-1", Duration.ofDays(14));

    verify(valueOperations).set("whitelist:refresh:{user-1}:jti-1", "valid", Duration.ofDays(14));
  }

  @Test
  void doesNotRegisterWhenTtlIsNotPositive() {
    refreshTokenWhitelist.register("user-1", "jti-1", Duration.ZERO);

    verify(redisTemplate, never()).opsForValue();
  }

  @Test
  void rejectsBlankTokenIdOnRegister() {
    assertThrows(
        IllegalArgumentException.class,
        () -> refreshTokenWhitelist.register("user-1", "  ", Duration.ofDays(14)));
  }

  @Test
  void rejectsBlankSubjectOnRegister() {
    assertThrows(
        IllegalArgumentException.class,
        () -> refreshTokenWhitelist.register("  ", "jti-1", Duration.ofDays(14)));
  }

  @Test
  void returnsTrueWhenKeyExists() {
    given(redisTemplate.hasKey("whitelist:refresh:{user-1}:jti-1")).willReturn(true);

    assertTrue(refreshTokenWhitelist.contains("user-1", "jti-1"));
  }

  @Test
  void returnsFalseWhenKeyAbsent() {
    given(redisTemplate.hasKey("whitelist:refresh:{user-1}:jti-1")).willReturn(false);

    assertFalse(refreshTokenWhitelist.contains("user-1", "jti-1"));
  }

  @Test
  void removesKey() {
    refreshTokenWhitelist.remove("user-1", "jti-1");

    verify(redisTemplate).delete("whitelist:refresh:{user-1}:jti-1");
  }

  @Test
  void doesNotQueryRedisForBlankTokenId() {
    assertFalse(refreshTokenWhitelist.contains("user-1", null));

    verify(redisTemplate, never()).hasKey(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void rotatesWhenOldTokenExists() {
    given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).willReturn(1L);

    assertTrue(refreshTokenWhitelist.rotate("user-1", "old", "new", Duration.ofDays(14)));
  }

  @Test
  @SuppressWarnings("unchecked")
  void doesNotRotateWhenOldTokenMissing() {
    given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).willReturn(0L);

    assertFalse(refreshTokenWhitelist.rotate("user-1", "old", "new", Duration.ofDays(14)));
  }

  @Test
  @SuppressWarnings("unchecked")
  void doesNotRotateWhenTtlIsNotPositive() {
    assertFalse(refreshTokenWhitelist.rotate("user-1", "old", "new", Duration.ZERO));

    verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any(), any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void rotateColocatesOldAndNewKeysUnderSameHashTag() {
    given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).willReturn(1L);
    ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);

    refreshTokenWhitelist.rotate("user-1", "old", "new", Duration.ofDays(14));

    verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), any(), any());
    assertEquals(
        List.of("whitelist:refresh:{user-1}:old", "whitelist:refresh:{user-1}:new"),
        keysCaptor.getValue());
  }
}
