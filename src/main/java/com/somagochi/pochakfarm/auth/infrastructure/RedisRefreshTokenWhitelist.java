package com.somagochi.pochakfarm.auth.infrastructure;

import com.somagochi.pochakfarm.auth.domain.RefreshTokenWhitelist;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRefreshTokenWhitelist implements RefreshTokenWhitelist {

  private static final String KEY_PREFIX = "whitelist:refresh:";
  private static final String MARKER = "valid";

  private static final RedisScript<Long> ROTATE_SCRIPT =
      RedisScript.of(
          "if redis.call('del', KEYS[1]) == 1 then "
              + "redis.call('set', KEYS[2], ARGV[1], 'PX', ARGV[2]); "
              + "return 1 "
              + "else return 0 end",
          Long.class);

  private final StringRedisTemplate redisTemplate;

  public RedisRefreshTokenWhitelist(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void register(String subject, String tokenId, Duration ttl) {
    if (isBlank(subject) || isBlank(tokenId)) {
      throw new IllegalArgumentException("subject and tokenId must not be blank");
    }
    if (isNotPositive(ttl)) {
      return;
    }
    redisTemplate.opsForValue().set(key(subject, tokenId), MARKER, ttl);
  }

  @Override
  public boolean contains(String subject, String tokenId) {
    if (isBlank(subject) || isBlank(tokenId)) {
      return false;
    }
    return Boolean.TRUE.equals(redisTemplate.hasKey(key(subject, tokenId)));
  }

  @Override
  public void remove(String subject, String tokenId) {
    if (isBlank(subject) || isBlank(tokenId)) {
      return;
    }
    redisTemplate.delete(key(subject, tokenId));
  }

  @Override
  public boolean rotate(String subject, String oldTokenId, String newTokenId, Duration ttl) {
    if (isBlank(subject) || isBlank(oldTokenId) || isBlank(newTokenId)) {
      return false;
    }
    if (isNotPositive(ttl)) {
      return false;
    }
    Long result =
        redisTemplate.execute(
            ROTATE_SCRIPT,
            List.of(key(subject, oldTokenId), key(subject, newTokenId)),
            MARKER,
            String.valueOf(ttl.toMillis()));
    return Long.valueOf(1L).equals(result);
  }

  // Redis Cluster hash tag({...})로 같은 subject의 키를 동일 슬롯에 배치한다.
  // rotate의 다중 키 스크립트가 CROSSSLOT 없이 실행되도록 하기 위함이다.
  private String key(String subject, String tokenId) {
    return KEY_PREFIX + "{" + subject + "}:" + tokenId;
  }

  private static boolean isBlank(String tokenId) {
    return tokenId == null || tokenId.isBlank();
  }

  private static boolean isNotPositive(Duration ttl) {
    return ttl == null || ttl.isNegative() || ttl.isZero();
  }
}
