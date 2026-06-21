package com.somagochi.pochakfarm.auth.infrastructure;

import com.somagochi.pochakfarm.auth.domain.RefreshTokenWhitelist;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRefreshTokenWhitelist implements RefreshTokenWhitelist {

  private static final String KEY_PREFIX = "whitelist:refresh:";
  private static final String MARKER = "valid";

  private final StringRedisTemplate redisTemplate;

  public RedisRefreshTokenWhitelist(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void register(String tokenId, Duration ttl) {
    if (tokenId == null || tokenId.isBlank()) {
      throw new IllegalArgumentException("tokenId must not be blank");
    }
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      return;
    }
    redisTemplate.opsForValue().set(key(tokenId), MARKER, ttl);
  }

  @Override
  public boolean contains(String tokenId) {
    if (tokenId == null || tokenId.isBlank()) {
      return false;
    }
    return Boolean.TRUE.equals(redisTemplate.hasKey(key(tokenId)));
  }

  @Override
  public void remove(String tokenId) {
    if (tokenId == null || tokenId.isBlank()) {
      return;
    }
    redisTemplate.delete(key(tokenId));
  }

  private String key(String tokenId) {
    return KEY_PREFIX + tokenId;
  }
}
