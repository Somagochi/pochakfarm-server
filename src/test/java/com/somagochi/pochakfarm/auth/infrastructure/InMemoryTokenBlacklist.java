package com.somagochi.pochakfarm.auth.infrastructure;

import com.somagochi.pochakfarm.auth.domain.TokenBlacklist;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class InMemoryTokenBlacklist implements TokenBlacklist {

  private final Set<String> blacklisted = new HashSet<>();

  @Override
  public void register(String tokenId, Duration ttl) {
    if (tokenId == null || tokenId.isBlank()) {
      throw new IllegalArgumentException("tokenId must not be blank");
    }
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      return;
    }
    blacklisted.add(tokenId);
  }

  @Override
  public boolean isBlacklisted(String tokenId) {
    if (tokenId == null || tokenId.isBlank()) {
      return false;
    }
    return blacklisted.contains(tokenId);
  }
}
