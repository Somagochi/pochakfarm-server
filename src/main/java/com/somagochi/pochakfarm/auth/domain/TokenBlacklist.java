package com.somagochi.pochakfarm.auth.domain;

import java.time.Duration;

public interface TokenBlacklist {

  /**
   * 토큰을 블랙리스트에 등록한다.
   *
   * @param tokenId 토큰 식별자(jti)
   * @param ttl 남은 만료 기간. 0 이하이면 이미 만료된 토큰이므로 등록하지 않는다.
   */
  void register(String tokenId, Duration ttl);

  /**
   * 토큰이 블랙리스트에 등록되어 있는지 확인한다.
   *
   * @param tokenId 토큰 식별자(jti)
   * @return 등록되어 있으면 true
   */
  boolean isBlacklisted(String tokenId);
}
