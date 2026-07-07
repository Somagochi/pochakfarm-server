package com.somagochi.pochakfarm.auth.application;

import com.somagochi.pochakfarm.auth.domain.RefreshTokenWhitelist;
import com.somagochi.pochakfarm.auth.domain.TokenBlacklist;
import com.somagochi.pochakfarm.auth.dto.TokenResponse;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.jwt.JwtExpiredException;
import com.somagochi.pochakfarm.common.jwt.JwtHelper;
import com.somagochi.pochakfarm.common.jwt.JwtInvalidException;
import com.somagochi.pochakfarm.common.jwt.JwtPayload;
import com.somagochi.pochakfarm.common.properties.JwtProperties;
import com.somagochi.pochakfarm.common.security.JwtAuthenticationException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

  private static final String JTI_CLAIM = "jti";
  private static final String TOKEN_TYPE_CLAIM = "tokenType";
  private static final String ACCESS_TOKEN_TYPE = "access";
  private static final String REFRESH_TOKEN_TYPE = "refresh";

  private final TokenBlacklist tokenBlacklist;
  private final RefreshTokenWhitelist refreshTokenWhitelist;
  private final JwtHelper jwtHelper;
  private final Duration accessTokenTtl;
  private final Duration refreshTokenTtl;

  public TokenService(
      JwtHelper jwtHelper,
      JwtProperties jwtProperties,
      TokenBlacklist tokenBlacklist,
      RefreshTokenWhitelist refreshTokenWhitelist) {
    this.jwtHelper = jwtHelper;
    this.tokenBlacklist = tokenBlacklist;
    this.refreshTokenWhitelist = refreshTokenWhitelist;
    this.accessTokenTtl =
        requirePositive(jwtProperties.accessTokenExpiration(), "accessTokenExpiration");
    this.refreshTokenTtl =
        requirePositive(jwtProperties.refreshTokenExpiration(), "refreshTokenExpiration");
  }

  public TokenResponse generateTokenPair(String subject) {
    return generateTokenPair(subject, Map.of(), Map.of());
  }

  public TokenResponse generateTokenPair(
      String subject,
      Map<String, Object> accessTokenClaims,
      Map<String, Object> refreshTokenClaims) {
    String accessToken = generateAccessToken(subject, accessTokenClaims);
    String refreshToken = generateRefreshToken(subject, refreshTokenClaims);
    return new TokenResponse(accessToken, refreshToken);
  }

  public String generateAccessToken(String subject) {
    return generateAccessToken(subject, Map.of());
  }

  public String generateAccessToken(String subject, Map<String, Object> claims) {
    String tokenId = UUID.randomUUID().toString();
    return jwtHelper.generateToken(
        subject, withReservedClaims(claims, ACCESS_TOKEN_TYPE, tokenId), accessTokenTtl);
  }

  public String generateRefreshToken(String subject) {
    return generateRefreshToken(subject, Map.of());
  }

  public String generateRefreshToken(String subject, Map<String, Object> claims) {
    String tokenId = UUID.randomUUID().toString();
    String refreshToken =
        jwtHelper.generateToken(
            subject, withReservedClaims(claims, REFRESH_TOKEN_TYPE, tokenId), refreshTokenTtl);
    refreshTokenWhitelist.register(tokenId, refreshTokenTtl);
    return refreshToken;
  }

  public JwtPayload parse(String token) {
    return parseOrThrow(token);
  }

  public JwtPayload parseAccessToken(String token) {
    JwtPayload payload = parseOrThrow(token);
    validateTokenType(payload, ACCESS_TOKEN_TYPE);
    return payload;
  }

  public JwtPayload verifyAccessToken(String token) {
    JwtPayload payload = parseAccessToken(token);
    if (tokenBlacklist.isBlacklisted(payload.tokenId())) {
      throw new JwtAuthenticationException(ErrorCode.BLACKLISTED_TOKEN);
    }
    return payload;
  }

  public JwtPayload parseRefreshToken(String token) {
    JwtPayload payload = parseOrThrow(token);
    validateTokenType(payload, REFRESH_TOKEN_TYPE);
    return payload;
  }

  public void revokeTokens(String accessToken, String refreshToken) {
    JwtPayload accessPayload = parseAccessToken(accessToken);
    JwtPayload refreshPayload = parseRefreshToken(refreshToken);
    if (!accessPayload.subject().equals(refreshPayload.subject())) {
      throw new JwtAuthenticationException(ErrorCode.TOKEN_OWNER_MISMATCH);
    }
    revokeRefreshToken(refreshToken);
    blacklistToken(accessToken);
  }

  public TokenResponse refreshTokens(JwtPayload oldRefreshPayload) {
    String subject = oldRefreshPayload.subject();
    String accessToken = generateAccessToken(subject);
    String newRefreshTokenId = UUID.randomUUID().toString();
    String newRefreshToken =
            jwtHelper.generateToken(
                    subject,
                    withReservedClaims(Map.of(), REFRESH_TOKEN_TYPE, newRefreshTokenId),
                    refreshTokenTtl);
    boolean rotated =
            refreshTokenWhitelist.rotate(
                    oldRefreshPayload.tokenId(), newRefreshTokenId, refreshTokenTtl);
    if (!rotated) {
      throw new JwtAuthenticationException(ErrorCode.REVOKED_REFRESH_TOKEN);
    }
    return new TokenResponse(accessToken, newRefreshToken);
  }

  public void blacklistToken(String token) {
    JwtPayload payload = parseAccessToken(token);
    Duration ttl = Duration.between(Instant.now(), payload.expiresAt());
    tokenBlacklist.register(payload.tokenId(), ttl);
  }

  public void revokeRefreshToken(String token) {
    JwtPayload payload = parseRefreshToken(token);
    refreshTokenWhitelist.remove(payload.tokenId());
  }

  private JwtPayload parseOrThrow(String token) {
    try {
      return jwtHelper.parse(token);
    } catch (JwtExpiredException exception) {
      throw new JwtAuthenticationException(ErrorCode.EXPIRED_TOKEN);
    } catch (JwtInvalidException exception) {
      throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
    }
  }

  private void validateTokenType(JwtPayload payload, String expectedType) {
    if (!expectedType.equals(payload.claims().get(TOKEN_TYPE_CLAIM))) {
      throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN_TYPE);
    }
  }

  private Map<String, Object> withReservedClaims(
      Map<String, Object> claims, String tokenType, String tokenId) {
    Map<String, Object> tokenClaims = new HashMap<>(claims);
    tokenClaims.put(JTI_CLAIM, tokenId);
    tokenClaims.put(TOKEN_TYPE_CLAIM, tokenType);
    return Map.copyOf(tokenClaims);
  }

  private Duration requirePositive(Duration duration, String fieldName) {
    if (duration == null || duration.isNegative() || duration.isZero()) {
      throw new IllegalArgumentException(fieldName + " must be greater than zero");
    }
    return duration;
  }
}
