package com.somagochi.pochakfarm.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.auth.dto.TokenResponse;
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
            jwtHelper, new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)));

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
  void throwsWhenTokenTypeDoesNotMatch() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    TokenService tokenService =
        new TokenService(
            jwtHelper, new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)));

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
            jwtHelper, new JwtProperties(SECRET, Duration.ofMillis(1), Duration.ofDays(14)));

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
