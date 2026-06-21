package com.somagochi.pochakfarm.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.auth.dto.TokenResponse;
import com.somagochi.pochakfarm.auth.infrastructure.InMemoryRefreshTokenWhitelist;
import com.somagochi.pochakfarm.auth.infrastructure.InMemoryTokenBlacklist;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.jwt.JwtHelper;
import com.somagochi.pochakfarm.common.jwt.JwtPayload;
import com.somagochi.pochakfarm.common.properties.JwtProperties;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationException;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

  private static final String SECRET = "12345678901234567890123456789012";

  @Test
  void generatesAccessAndRefreshTokensWithDifferentPolicies() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    TokenService tokenService =
        new TokenService(
            jwtHelper,
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)),
            new InMemoryTokenBlacklist(),
            new InMemoryRefreshTokenWhitelist());

    TokenResponse tokenResponse =
        tokenService.generateTokenPair(
            "user-1", Map.of("role", "USER"), Map.of("deviceId", "device-1"));

    JwtPayload accessPayload = tokenService.parse(tokenResponse.accessToken());
    JwtPayload refreshPayload = tokenService.parse(tokenResponse.refreshToken());

    assertNotEquals(tokenResponse.accessToken(), tokenResponse.refreshToken());
    assertEquals("user-1", accessPayload.subject());
    assertEquals("user-1", refreshPayload.subject());
    assertNotEquals(accessPayload.tokenId(), refreshPayload.tokenId());
    assertEquals(accessPayload.tokenId(), accessPayload.claims().get("jti"));
    assertEquals(refreshPayload.tokenId(), refreshPayload.claims().get("jti"));
    assertEquals("access", accessPayload.claims().get("tokenType"));
    assertEquals("refresh", refreshPayload.claims().get("tokenType"));
    assertEquals("USER", accessPayload.claims().get("role"));
    assertEquals("device-1", refreshPayload.claims().get("deviceId"));
    assertEquals(
        Duration.ofMinutes(30),
        Duration.between(accessPayload.issuedAt(), accessPayload.expiresAt()));
    assertEquals(
        Duration.ofDays(14),
        Duration.between(refreshPayload.issuedAt(), refreshPayload.expiresAt()));
    assertEquals(accessPayload, tokenService.parseAccessToken(tokenResponse.accessToken()));
    assertEquals(refreshPayload, tokenService.parseRefreshToken(tokenResponse.refreshToken()));
  }

  @Test
  void registersRefreshTokenInWhitelistWhenGenerated() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    InMemoryRefreshTokenWhitelist refreshTokenWhitelist = new InMemoryRefreshTokenWhitelist();
    TokenService tokenService =
        new TokenService(
            jwtHelper,
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)),
            new InMemoryTokenBlacklist(),
            refreshTokenWhitelist);

    String refreshToken = tokenService.generateRefreshToken("user-1");
    String jti = tokenService.parseRefreshToken(refreshToken).tokenId();

    assertTrue(refreshTokenWhitelist.contains(jti));
  }

  @Test
  void revokeRefreshTokenRemovesFromWhitelist() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    InMemoryRefreshTokenWhitelist refreshTokenWhitelist = new InMemoryRefreshTokenWhitelist();
    TokenService tokenService =
        new TokenService(
            jwtHelper,
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)),
            new InMemoryTokenBlacklist(),
            refreshTokenWhitelist);

    String refreshToken = tokenService.generateRefreshToken("user-1");
    String jti = tokenService.parseRefreshToken(refreshToken).tokenId();
    assertTrue(refreshTokenWhitelist.contains(jti));

    tokenService.revokeRefreshToken(refreshToken);

    assertFalse(refreshTokenWhitelist.contains(jti));
  }

  @Test
  void blacklistsAccessTokenByJti() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    InMemoryTokenBlacklist tokenBlacklist = new InMemoryTokenBlacklist();
    TokenService tokenService =
        new TokenService(
            jwtHelper,
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)),
            tokenBlacklist,
            new InMemoryRefreshTokenWhitelist());

    String accessToken = tokenService.generateAccessToken("user-1");
    String jti = tokenService.parseAccessToken(accessToken).tokenId();
    assertFalse(tokenBlacklist.isBlacklisted(jti));

    tokenService.blacklistToken(accessToken);

    assertTrue(tokenBlacklist.isBlacklisted(jti));
  }

  @Test
  void verifyAccessTokenReturnsPayloadWhenNotBlacklisted() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    TokenService tokenService =
        new TokenService(
            jwtHelper,
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)),
            new InMemoryTokenBlacklist(),
            new InMemoryRefreshTokenWhitelist());

    String accessToken = tokenService.generateAccessToken("user-1");

    JwtPayload payload = tokenService.verifyAccessToken(accessToken);

    assertEquals("user-1", payload.subject());
  }

  @Test
  void verifyAccessTokenThrowsWhenBlacklisted() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    InMemoryTokenBlacklist tokenBlacklist = new InMemoryTokenBlacklist();
    TokenService tokenService =
        new TokenService(
            jwtHelper,
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)),
            tokenBlacklist,
            new InMemoryRefreshTokenWhitelist());

    String accessToken = tokenService.generateAccessToken("user-1");
    tokenService.blacklistToken(accessToken);

    JwtAuthenticationException exception =
        assertThrows(
            JwtAuthenticationException.class, () -> tokenService.verifyAccessToken(accessToken));

    assertEquals(ErrorCode.BLACKLISTED_TOKEN.getCode(), exception.getCode());
  }

  @Test
  void throwsWhenBlacklistingNonAccessToken() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    TokenService tokenService =
        new TokenService(
            jwtHelper,
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)),
            new InMemoryTokenBlacklist(),
            new InMemoryRefreshTokenWhitelist());

    String refreshToken = tokenService.generateRefreshToken("user-1");

    JwtAuthenticationException exception =
        assertThrows(
            JwtAuthenticationException.class, () -> tokenService.blacklistToken(refreshToken));

    assertEquals(ErrorCode.INVALID_TOKEN_TYPE.getCode(), exception.getCode());
  }

  @Test
  void throwsWhenTokenTypeDoesNotMatch() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    TokenService tokenService =
        new TokenService(
            jwtHelper,
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)),
            new InMemoryTokenBlacklist(),
            new InMemoryRefreshTokenWhitelist());

    String refreshToken = tokenService.generateRefreshToken("user-1");

    JwtAuthenticationException exception =
        assertThrows(
            JwtAuthenticationException.class, () -> tokenService.parseAccessToken(refreshToken));

    assertEquals(ErrorCode.INVALID_TOKEN_TYPE.getCode(), exception.getCode());
  }

  @Test
  void throwsExpiredTokenWhenAccessTokenIsExpired() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    TokenService tokenService =
        new TokenService(
            jwtHelper,
            new JwtProperties(SECRET, Duration.ofMillis(1), Duration.ofDays(14)),
            new InMemoryTokenBlacklist(),
            new InMemoryRefreshTokenWhitelist());

    String accessToken = tokenService.generateAccessToken("user-1");

    try {
      Thread.sleep(10L);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(exception);
    }

    JwtAuthenticationException exception =
        assertThrows(
            JwtAuthenticationException.class, () -> tokenService.parseAccessToken(accessToken));

    assertEquals(ErrorCode.EXPIRED_TOKEN.getCode(), exception.getCode());
  }
}
