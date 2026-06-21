package com.somagochi.pochakfarm.auth.domain;

import java.time.Duration;

public interface RefreshTokenWhitelist {

  void register(String tokenId, Duration ttl);

  boolean contains(String tokenId);

  void remove(String tokenId);
}
