package com.somagochi.pochakfarm.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;

public final class JwtHelper {

  private final SecretKey signingKey;
  private final JwtParser jwtParser;

  public JwtHelper(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("secret must not be blank");
    }

    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);

    this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    this.jwtParser = Jwts.parser().verifyWith(signingKey).build();
  }

  public String generateToken(String subject, Duration ttl) {
    return generateToken(subject, Map.of(), ttl);
  }

  public String generateToken(String subject, Map<String, Object> claims, Duration ttl) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("subject must not be blank");
    }
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      throw new IllegalArgumentException("ttl must be greater than zero");
    }

    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plus(ttl);

    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .signWith(signingKey)
        .compact();
  }

  public JwtPayload parse(String token) {
    Claims claims = parseClaims(token);
    return toPayload(claims);
  }

  public String extractSubject(String token) {
    return parse(token).subject();
  }

  public Instant extractExpiration(String token) {
    try {
      return parse(token).expiresAt();
    } catch (JwtExpiredException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof ExpiredJwtException expiredJwtException) {
        return expiredJwtException.getClaims().getExpiration().toInstant();
      }
      throw exception;
    }
  }

  public boolean isValid(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtInvalidException exception) {
      return false;
    }
  }

  public boolean isExpired(String token) {
    try {
      parse(token);
      return false;
    } catch (JwtExpiredException exception) {
      return true;
    }
  }

  private Claims parseClaims(String token) {
    try {
      return jwtParser.parseSignedClaims(token).getPayload();
    } catch (ExpiredJwtException exception) {
      throw new JwtExpiredException(exception);
    } catch (JwtException | IllegalArgumentException exception) {
      throw new JwtInvalidException(exception);
    }
  }

  private JwtPayload toPayload(Claims claims) {
    return new JwtPayload(
        claims.getId(),
        claims.getSubject(),
        claims.getIssuedAt().toInstant(),
        claims.getExpiration().toInstant(),
        Map.copyOf(claims));
  }
}
