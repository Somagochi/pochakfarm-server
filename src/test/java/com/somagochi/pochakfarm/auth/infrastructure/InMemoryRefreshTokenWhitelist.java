package com.somagochi.pochakfarm.auth.infrastructure;

import com.somagochi.pochakfarm.auth.domain.RefreshTokenWhitelist;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class InMemoryRefreshTokenWhitelist implements RefreshTokenWhitelist {

  private final Set<String> whitelisted = new HashSet<>();

  @Override
  public void register(String tokenId, Duration ttl) {
    if (tokenId == null || tokenId.isBlank()) {
      throw new IllegalArgumentException("tokenId must not be blank");
    }
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
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
}
