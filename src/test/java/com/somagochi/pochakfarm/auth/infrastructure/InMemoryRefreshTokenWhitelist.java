package com.somagochi.pochakfarm.auth.infrastructure;

import com.somagochi.pochakfarm.auth.domain.RefreshTokenWhitelist;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class InMemoryRefreshTokenWhitelist implements RefreshTokenWhitelist {

  private final Set<String> whitelisted = new HashSet<>();

  @Override
  public void register(String subject, String tokenId, Duration ttl) {
    if (isBlank(subject) || isBlank(tokenId)) {
      throw new IllegalArgumentException("subject and tokenId must not be blank");
    }
    if (isNotPositive(ttl)) {
      return;
    }
    whitelisted.add(key(subject, tokenId));
  }

  @Override
  public boolean contains(String subject, String tokenId) {
    return subject != null && tokenId != null && whitelisted.contains(key(subject, tokenId));
  }

  @Override
  public void remove(String subject, String tokenId) {
    whitelisted.remove(key(subject, tokenId));
  }

  @Override
  public synchronized boolean rotate(
      String subject, String oldTokenId, String newTokenId, Duration ttl) {
    if (isBlank(subject) || isBlank(newTokenId)) {
      return false;
    }
    if (isNotPositive(ttl)) {
      return false;
    }
    if (!whitelisted.remove(key(subject, oldTokenId))) {
      return false;
    }
    whitelisted.add(key(subject, newTokenId));
    return true;
  }

  private static String key(String subject, String tokenId) {
    return subject + "|" + tokenId;
  }

  private static boolean isBlank(String tokenId) {
    return tokenId == null || tokenId.isBlank();
  }

  private static boolean isNotPositive(Duration ttl) {
    return ttl == null || ttl.isNegative() || ttl.isZero();
  }
}
