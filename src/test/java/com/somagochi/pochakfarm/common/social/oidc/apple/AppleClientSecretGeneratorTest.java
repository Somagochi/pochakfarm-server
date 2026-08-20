package com.somagochi.pochakfarm.common.social.oidc.apple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.AppleOAuthProperties;
import com.somagochi.pochakfarm.common.social.oauth2.apple.AppleClientSecretGenerator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

class AppleClientSecretGeneratorTest {

  private static final String TEAM_ID = "TEAM123456";
  private static final String KEY_ID = "KEY1234567";
  private static final String CLIENT_ID = "com.somagochi.pochakfarm.web";

  private static KeyPair keyPair;

  @TempDir static Path tempDir;

  @BeforeAll
  static void generateKeyPair() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
    generator.initialize(new ECGenParameterSpec("secp256r1"));
    keyPair = generator.generateKeyPair();
  }

  @Test
  void signsClientSecretWithConfiguredKeyAndClaims() {
    AppleClientSecretGenerator secretGenerator =
        generatorWith(writeKeyFile("plain.p8", base64PrivateKey()));
    Instant before = Instant.now();

    Jws<Claims> jws = parse(secretGenerator.generate());

    assertEquals("ES256", jws.getHeader().getAlgorithm());
    assertEquals(KEY_ID, jws.getHeader().getKeyId());
    assertEquals(TEAM_ID, jws.getPayload().getIssuer());
    assertEquals(CLIENT_ID, jws.getPayload().getSubject());
    assertTrue(jws.getPayload().getAudience().contains("https://appleid.apple.com"));
    assertTrue(jws.getPayload().getExpiration().toInstant().isAfter(before));
  }

  @Test
  void expiresThirtyMinutesAfterIssuedAt() {
    Jws<Claims> jws = parse(generatorWith(writeKeyFile("plain.p8", base64PrivateKey())).generate());

    Instant issuedAt = jws.getPayload().getIssuedAt().toInstant();
    Instant expiration = jws.getPayload().getExpiration().toInstant();

    assertEquals(Duration.ofMinutes(30), Duration.between(issuedAt, expiration));
  }

  @Test
  void acceptsPrivateKeyWithPemHeaders() {
    String pem =
        "-----BEGIN PRIVATE KEY-----\n" + base64PrivateKey() + "\n-----END PRIVATE KEY-----\n";

    Jws<Claims> jws = parse(generatorWith(writeKeyFile("pem.p8", pem)).generate());

    assertEquals(TEAM_ID, jws.getPayload().getIssuer());
  }

  @Test
  void reusesParsedPrivateKeyAcrossCalls() {
    AppleClientSecretGenerator secretGenerator =
        generatorWith(writeKeyFile("plain.p8", base64PrivateKey()));

    assertEquals(TEAM_ID, parse(secretGenerator.generate()).getPayload().getIssuer());
    assertEquals(TEAM_ID, parse(secretGenerator.generate()).getPayload().getIssuer());
  }

  @Test
  void throwsWhenPrivateKeyPathIsNotConfigured() {
    AppleClientSecretGenerator secretGenerator = generatorWith("  ");

    BusinessException exception = assertThrows(BusinessException.class, secretGenerator::generate);

    assertEquals(ErrorCode.APPLE_CLIENT_SECRET_FAILED.getCode(), exception.getCode());
  }

  @Test
  void throwsWhenPrivateKeyFileIsMissing() {
    AppleClientSecretGenerator secretGenerator =
        generatorWith("classpath:secret/does-not-exist.p8");

    BusinessException exception = assertThrows(BusinessException.class, secretGenerator::generate);

    assertEquals(ErrorCode.APPLE_CLIENT_SECRET_FAILED.getCode(), exception.getCode());
  }

  @Test
  void throwsWhenPrivateKeyFileIsMalformed() {
    AppleClientSecretGenerator secretGenerator = generatorWith(writeKeyFile("bad.p8", "not-a-key"));

    BusinessException exception = assertThrows(BusinessException.class, secretGenerator::generate);

    assertEquals(ErrorCode.APPLE_CLIENT_SECRET_FAILED.getCode(), exception.getCode());
  }

  private static String base64PrivateKey() {
    return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
  }

  private static String writeKeyFile(String name, String content) {
    try {
      Path path = tempDir.resolve(name);
      Files.writeString(path, content);
      return path.toUri().toString();
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static AppleClientSecretGenerator generatorWith(String privateKeyPath) {
    return new AppleClientSecretGenerator(
        new AppleOAuthProperties(
            CLIENT_ID,
            TEAM_ID,
            KEY_ID,
            privateKeyPath,
            "https://api.example.com/api/auth/oauth2/code/apple",
            "https://appleid.apple.com/auth/token"),
        new DefaultResourceLoader());
  }

  private static Jws<Claims> parse(String clientSecret) {
    return Jwts.parser().verifyWith(keyPair.getPublic()).build().parseSignedClaims(clientSecret);
  }
}
