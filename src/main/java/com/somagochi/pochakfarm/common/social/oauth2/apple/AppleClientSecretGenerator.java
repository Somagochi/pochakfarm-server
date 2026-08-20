package com.somagochi.pochakfarm.common.social.oauth2.apple;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.AppleOAuthProperties;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Pattern;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class AppleClientSecretGenerator {

  private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
  private static final Duration TTL = Duration.ofMinutes(30);
  private static final Pattern PEM_DECORATION = Pattern.compile("-----[^-]+-----|\\s");

  private final AppleOAuthProperties appleOAuthProperties;
  private final ResourceLoader resourceLoader;

  private volatile PrivateKey privateKey;

  public AppleClientSecretGenerator(
      AppleOAuthProperties appleOAuthProperties, ResourceLoader resourceLoader) {
    this.appleOAuthProperties = appleOAuthProperties;
    this.resourceLoader = resourceLoader;
  }

  public String generate() {
    Instant issuedAt = Instant.now();
    return Jwts.builder()
        .header()
        .keyId(required(appleOAuthProperties.keyId()))
        .and()
        .issuer(required(appleOAuthProperties.teamId()))
        .subject(required(appleOAuthProperties.clientId()))
        .audience()
        .add(APPLE_AUDIENCE)
        .and()
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(issuedAt.plus(TTL)))
        .signWith(privateKey(), Jwts.SIG.ES256)
        .compact();
  }

  private PrivateKey privateKey() {
    PrivateKey cached = privateKey;
    if (cached != null) {
      return cached;
    }
    synchronized (this) {
      if (privateKey == null) {
        privateKey = loadPrivateKey(required(appleOAuthProperties.privateKeyPath()));
      }
      return privateKey;
    }
  }

  private PrivateKey loadPrivateKey(String location) {
    try {
      String content =
          resourceLoader.getResource(location).getContentAsString(StandardCharsets.UTF_8);
      byte[] decoded = Base64.getDecoder().decode(PEM_DECORATION.matcher(content).replaceAll(""));
      return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    } catch (IOException | GeneralSecurityException | IllegalArgumentException exception) {
      throw new BusinessException(ErrorCode.APPLE_CLIENT_SECRET_FAILED);
    }
  }

  private String required(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(ErrorCode.APPLE_CLIENT_SECRET_FAILED);
    }
    return value;
  }
}
