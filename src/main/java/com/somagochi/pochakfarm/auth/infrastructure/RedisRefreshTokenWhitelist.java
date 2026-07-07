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
  public void register(String tokenId, Duration ttl) {
    if (isBlank(tokenId)) {
      throw new IllegalArgumentException("tokenId must not be blank");
    }
    if (isNotPositive(ttl)) {
      return;
    }
    redisTemplate.opsForValue().set(key(tokenId), MARKER, ttl);
  }

  @Override
  public boolean contains(String tokenId) {
    if (isBlank(tokenId)) {
      return false;
    }
    return Boolean.TRUE.equals(redisTemplate.hasKey(key(tokenId)));
  }

  @Override
  public void remove(String tokenId) {
    if (isBlank(tokenId)) {
      return;
    }
    redisTemplate.delete(key(tokenId));
  }

  @Override
  public boolean rotate(String oldTokenId, String newTokenId, Duration ttl) {
    if (isBlank(oldTokenId) || isBlank(newTokenId)) {
      return false;
    }
    if (isNotPositive(ttl)) {
      return false;
    }
    Long result =
        redisTemplate.execute(
            ROTATE_SCRIPT,
            List.of(key(oldTokenId), key(newTokenId)),
            MARKER,
            String.valueOf(ttl.toMillis()));
    return Long.valueOf(1L).equals(result);
  }

  private String key(String tokenId) {
    return KEY_PREFIX + tokenId;
  }

  private static boolean isBlank(String tokenId) {
    return tokenId == null || tokenId.isBlank();
  }

  private static boolean isNotPositive(Duration ttl) {
    return ttl == null || ttl.isNegative() || ttl.isZero();
  }
}
