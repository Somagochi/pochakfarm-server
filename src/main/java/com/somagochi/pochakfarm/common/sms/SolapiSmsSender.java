package com.somagochi.pochakfarm.common.sms;

import com.somagochi.pochakfarm.common.properties.SolapiProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class SolapiSmsSender implements SmsSender {

  private static final String SEND_PATH = "/messages/v4/send";
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final int SALT_LENGTH_BYTES = 16;

  private final SolapiProperties properties;
  private final RestClient restClient;
  private final SecureRandom secureRandom = new SecureRandom();

  @Autowired
  public SolapiSmsSender(SolapiProperties properties) {
    this(properties, RestClient.builder());
  }

  SolapiSmsSender(SolapiProperties properties, RestClient.Builder restClientBuilder) {
    this.properties = properties;
    this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
  }

  @Override
  public void send(String to, String text) {
    validateConfigured();
    restClient
        .post()
        .uri(SEND_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION, createAuthorizationHeader())
        .body(SolapiSendRequest.of(to, properties.from(), text))
        .retrieve()
        .toBodilessEntity();
    log.info("solapi_sms_sent textLength={}", text.length());
  }

  private void validateConfigured() {
    if (isBlank(properties.apiKey()) || isBlank(properties.apiSecret())) {
      throw new IllegalStateException("Solapi API credentials are not configured");
    }
    if (isBlank(properties.from())) {
      throw new IllegalStateException("Solapi from number is not configured");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String createAuthorizationHeader() {
    String date = Instant.now().toString();
    String salt = generateSalt();
    String signature = hmacSha256Hex(properties.apiSecret(), date + salt);
    return "HMAC-SHA256 apiKey=%s, date=%s, salt=%s, signature=%s"
        .formatted(properties.apiKey(), date, salt, signature);
  }

  private String generateSalt() {
    byte[] salt = new byte[SALT_LENGTH_BYTES];
    secureRandom.nextBytes(salt);
    return HexFormat.of().formatHex(salt);
  }

  private String hmacSha256Hex(String secret, String message) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Failed to create Solapi signature", exception);
    }
  }
}
