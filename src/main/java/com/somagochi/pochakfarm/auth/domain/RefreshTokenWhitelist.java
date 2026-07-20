package com.somagochi.pochakfarm.auth.domain;

import java.time.Duration;

public interface RefreshTokenWhitelist {

  void register(String subject, String tokenId, Duration ttl);

  boolean contains(String subject, String tokenId);

  void remove(String subject, String tokenId);

  boolean rotate(String subject, String oldTokenId, String newTokenId, Duration ttl);
}
