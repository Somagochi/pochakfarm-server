package com.somagochi.pochakfarm.auth.application;

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
import com.somagochi.pochakfarm.common.properties.JwtProperties;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RefreshServiceTest {

  private static final String SECRET = "12345678901234567890123456789012";

  private TokenService newTokenService(InMemoryRefreshTokenWhitelist refreshTokenWhitelist) {
    return new TokenService(
        new JwtHelper(SECRET),
        new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)),
        new InMemoryTokenBlacklist(),
        refreshTokenWhitelist);
  }

  @Test
  void rotatesRefreshTokenAndIssuesNewPair() {
    InMemoryRefreshTokenWhitelist refreshTokenWhitelist = new InMemoryRefreshTokenWhitelist();
    TokenService tokenService = newTokenService(refreshTokenWhitelist);
    RefreshService refreshService = new RefreshService(tokenService);

    String oldRefreshToken = tokenService.generateRefreshToken("1");
    String oldRefreshTokenId = tokenService.parseRefreshToken(oldRefreshToken).tokenId();

    TokenResponse refreshed = refreshService.refresh(oldRefreshToken);

    String newRefreshTokenId = tokenService.parseRefreshToken(refreshed.refreshToken()).tokenId();
    assertEquals("1", tokenService.verifyAccessToken(refreshed.accessToken()).subject());
    assertNotEquals(oldRefreshTokenId, newRefreshTokenId);
    assertFalse(refreshTokenWhitelist.contains("1", oldRefreshTokenId));
    assertTrue(refreshTokenWhitelist.contains("1", newRefreshTokenId));
  }

  @Test
  void rejectsReusedRefreshTokenAfterRotation() {
    InMemoryRefreshTokenWhitelist refreshTokenWhitelist = new InMemoryRefreshTokenWhitelist();
    TokenService tokenService = newTokenService(refreshTokenWhitelist);
    RefreshService refreshService = new RefreshService(tokenService);

    String oldRefreshToken = tokenService.generateRefreshToken("1");
    refreshService.refresh(oldRefreshToken);

    JwtAuthenticationException exception =
        assertThrows(
            JwtAuthenticationException.class, () -> refreshService.refresh(oldRefreshToken));

    assertEquals(ErrorCode.REVOKED_REFRESH_TOKEN.getCode(), exception.getCode());
  }

  @Test
  void rejectsAccessTokenAsRefreshToken() {
    InMemoryRefreshTokenWhitelist refreshTokenWhitelist = new InMemoryRefreshTokenWhitelist();
    TokenService tokenService = newTokenService(refreshTokenWhitelist);
    RefreshService refreshService = new RefreshService(tokenService);

    String accessToken = tokenService.generateAccessToken("1");

    JwtAuthenticationException exception =
        assertThrows(JwtAuthenticationException.class, () -> refreshService.refresh(accessToken));

    assertEquals(ErrorCode.INVALID_TOKEN_TYPE.getCode(), exception.getCode());
  }
}
