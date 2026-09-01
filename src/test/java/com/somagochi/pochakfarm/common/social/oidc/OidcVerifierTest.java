package com.somagochi.pochakfarm.common.social.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OidcVerifierTest {

  private static final String ISSUER = "https://appleid.apple.com";
  private static final String APP_AUDIENCE = "com.example.app";
  private static final String WEB_AUDIENCE = "com.example.app.web";
  private static final List<String> AUDIENCES = List.of(APP_AUDIENCE, WEB_AUDIENCE);
  private static final String JWKS_URI = "https://appleid.apple.com/auth/keys";
  private static final String KID = "test-kid";
  private static final String SUBJECT = "apple-user-123";
  private static final String EMAIL = "user@privaterelay.appleid.com";

  private KeyPair keyPair;
  private KeyPair otherKeyPair;
  private OidcVerifier oidcVerifier;

  @BeforeEach
  void setUp() {
    keyPair = generateRsaKeyPair();
    otherKeyPair = generateRsaKeyPair();

    JwksProvider jwksProvider = mock(JwksProvider.class);
    when(jwksProvider.findKey(JWKS_URI, KID)).thenReturn(keyPair.getPublic());

    oidcVerifier = new OidcVerifier(jwksProvider);
  }

  @Test
  void returnsClaimsForAppAudience() {
    String token =
        token(ISSUER, APP_AUDIENCE, Instant.now().plusSeconds(300), keyPair.getPrivate());

    Claims claims = oidcVerifier.verify(token, ISSUER, AUDIENCES, JWKS_URI);

    assertEquals(SUBJECT, claims.getSubject());
    assertEquals(EMAIL, claims.get("email", String.class));
    assertEquals(ISSUER, claims.getIssuer());
  }

  @Test
  void returnsClaimsForWebAudience() {
    String token =
        token(ISSUER, WEB_AUDIENCE, Instant.now().plusSeconds(300), keyPair.getPrivate());

    Claims claims = oidcVerifier.verify(token, ISSUER, AUDIENCES, JWKS_URI);

    assertEquals(SUBJECT, claims.getSubject());
  }

  @Test
  void throwsWhenIssuerDoesNotMatch() {
    String token =
        token(
            "https://evil.example.com",
            APP_AUDIENCE,
            Instant.now().plusSeconds(300),
            keyPair.getPrivate());

    assertInvalidSocialToken(token);
  }

  @Test
  void throwsWhenAudienceDoesNotMatch() {
    String token =
        token(ISSUER, "com.attacker.app", Instant.now().plusSeconds(300), keyPair.getPrivate());

    assertInvalidSocialToken(token);
  }

  @Test
  void throwsWhenTokenExpired() {
    String token =
        token(ISSUER, APP_AUDIENCE, Instant.now().minusSeconds(60), keyPair.getPrivate());

    assertInvalidSocialToken(token);
  }

  @Test
  void throwsWhenSignatureDoesNotMatchJwksKey() {
    // 토큰은 다른 개인키로 서명했지만 JwksProvider는 원래 공개키를 돌려준다 → 서명 불일치
    String token =
        token(ISSUER, APP_AUDIENCE, Instant.now().plusSeconds(300), otherKeyPair.getPrivate());

    assertInvalidSocialToken(token);
  }

  @Test
  void throwsWhenTokenMalformed() {
    assertInvalidSocialToken("not-a-jwt");
  }

  private void assertInvalidSocialToken(String token) {
    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> oidcVerifier.verify(token, ISSUER, AUDIENCES, JWKS_URI));

    assertEquals(ErrorCode.INVALID_SOCIAL_TOKEN.getCode(), exception.getCode());
  }

  private String token(String issuer, String audience, Instant expiration, PrivateKey signingKey) {
    return Jwts.builder()
        .header()
        .keyId(KID)
        .and()
        .issuer(issuer)
        .audience()
        .add(audience)
        .and()
        .subject(SUBJECT)
        .claim("email", EMAIL)
        .issuedAt(Date.from(Instant.now().minusSeconds(10)))
        .expiration(Date.from(expiration))
        .signWith(signingKey, Jwts.SIG.RS256)
        .compact();
  }

  private static KeyPair generateRsaKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
