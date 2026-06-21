package com.somagochi.pochakfarm.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.auth.infrastructure.InMemoryRefreshTokenWhitelist;
import com.somagochi.pochakfarm.auth.infrastructure.InMemoryTokenBlacklist;
import com.somagochi.pochakfarm.auth.service.TokenService;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.jwt.JwtHelper;
import com.somagochi.pochakfarm.common.properties.JwtProperties;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LogoutServiceTest {

  private static final String SECRET = "12345678901234567890123456789012";

  @Test
  void blacklistsAccessTokenAndRemovesRefreshTokenFromWhitelist() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    InMemoryTokenBlacklist tokenBlacklist = new InMemoryTokenBlacklist();
    InMemoryRefreshTokenWhitelist refreshTokenWhitelist = new InMemoryRefreshTokenWhitelist();
    TokenService tokenService =
        new TokenService(
            jwtHelper,
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)),
            tokenBlacklist,
            refreshTokenWhitelist);
    LogoutService logoutService = new LogoutService(tokenService);

    String accessToken = tokenService.generateAccessToken("1");
    String refreshToken = tokenService.generateRefreshToken("1");
    String accessTokenId = tokenService.parseAccessToken(accessToken).tokenId();
    String refreshTokenId = tokenService.parseRefreshToken(refreshToken).tokenId();
    assertTrue(refreshTokenWhitelist.contains(refreshTokenId));

    logoutService.logout(accessToken, refreshToken);

    assertTrue(tokenBlacklist.isBlacklisted(accessTokenId));
    assertFalse(refreshTokenWhitelist.contains(refreshTokenId));
  }

  @Test
  void throwsWhenAccessAndRefreshBelongToDifferentUsers() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    InMemoryTokenBlacklist tokenBlacklist = new InMemoryTokenBlacklist();
    InMemoryRefreshTokenWhitelist refreshTokenWhitelist = new InMemoryRefreshTokenWhitelist();
    TokenService tokenService =
        new TokenService(
            jwtHelper,
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)),
            tokenBlacklist,
            refreshTokenWhitelist);
    LogoutService logoutService = new LogoutService(tokenService);

    String accessToken = tokenService.generateAccessToken("1");
    String refreshToken = tokenService.generateRefreshToken("2");
    String accessTokenId = tokenService.parseAccessToken(accessToken).tokenId();
    String refreshTokenId = tokenService.parseRefreshToken(refreshToken).tokenId();

    JwtAuthenticationException exception =
        assertThrows(
            JwtAuthenticationException.class,
            () -> logoutService.logout(accessToken, refreshToken));

    assertEquals(ErrorCode.TOKEN_OWNER_MISMATCH.getCode(), exception.getCode());
    assertFalse(tokenBlacklist.isBlacklisted(accessTokenId));
    assertTrue(refreshTokenWhitelist.contains(refreshTokenId));
  }
}
