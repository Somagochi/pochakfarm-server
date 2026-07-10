package com.somagochi.pochakfarm.common.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtHelperTest {

  private static final String SECRET = "12345678901234567890123456789012";

  @Test
  void generatesTokenAndParsesClaims() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);
    Instant beforeGeneration = Instant.now();

    String token =
        jwtHelper.generateToken(
            "user-1", Map.of("role", "USER", "jti", "token-1"), Duration.ofMinutes(30));
    JwtPayload payload = jwtHelper.parse(token);
    Instant afterGeneration = Instant.now();

    assertTrue(jwtHelper.isValid(token));
    assertFalse(jwtHelper.isExpired(token));
    assertEquals("token-1", payload.tokenId());
    assertEquals("user-1", jwtHelper.extractSubject(token));
    assertEquals("USER", payload.claims().get("role"));
    assertTrue(!payload.issuedAt().isBefore(beforeGeneration.minusSeconds(1)));
    assertTrue(!payload.issuedAt().isAfter(afterGeneration.plusSeconds(1)));
    assertEquals(payload.expiresAt(), jwtHelper.extractExpiration(token));
    assertEquals(Duration.ofMinutes(30), Duration.between(payload.issuedAt(), payload.expiresAt()));
  }

  @Test
  void returnsExpiredForExpiredToken() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);

    String token = jwtHelper.generateToken("user-1", Duration.ofMillis(1));

    try {
      Thread.sleep(10L);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(exception);
    }

    assertFalse(jwtHelper.isValid(token));
    assertTrue(jwtHelper.isExpired(token));
  }

  @Test
  void throwsJwtExpiredExceptionWhenParsingExpiredToken() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);

    String token = jwtHelper.generateToken("user-1", Duration.ofMillis(1));

    try {
      Thread.sleep(10L);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(exception);
    }

    assertThrows(JwtExpiredException.class, () -> jwtHelper.parse(token));
  }

  @Test
  void throwsJwtInvalidExceptionWhenParsingInvalidToken() {
    JwtHelper jwtHelper = new JwtHelper(SECRET);

    assertThrows(JwtInvalidException.class, () -> jwtHelper.parse("invalid-token"));
  }
}
