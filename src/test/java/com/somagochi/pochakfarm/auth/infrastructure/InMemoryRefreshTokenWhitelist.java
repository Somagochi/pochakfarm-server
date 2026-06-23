package com.somagochi.pochakfarm.auth.infrastructure;

import com.somagochi.pochakfarm.auth.domain.RefreshTokenWhitelist;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class InMemoryRefreshTokenWhitelist implements RefreshTokenWhitelist {

  private final Set<String> whitelisted = new HashSet<>();

  @Override
  public void register(String tokenId, Duration ttl) {
    if (isBlank(tokenId)) {
      throw new IllegalArgumentException("tokenId must not be blank");
    }
    if (isNotPositive(ttl)) {
      return;
    }
    whitelisted.add(tokenId);
  }

  @Override
  public boolean contains(String tokenId) {
    return tokenId != null && whitelisted.contains(tokenId);
  }

  @Override
  public void remove(String tokenId) {
    whitelisted.remove(tokenId);
  }

  @Override
  public synchronized boolean rotate(String oldTokenId, String newTokenId, Duration ttl) {
    if (isBlank(newTokenId)) {
      return false;
    }
    if (isNotPositive(ttl)) {
      return false;
    }
    if (!whitelisted.remove(oldTokenId)) {
      return false;
    }
    whitelisted.add(newTokenId);
    return true;
  }

  private static boolean isBlank(String tokenId) {
    return tokenId == null || tokenId.isBlank();
  }

  private static boolean isNotPositive(Duration ttl) {
    return ttl == null || ttl.isNegative() || ttl.isZero();
  }
}
