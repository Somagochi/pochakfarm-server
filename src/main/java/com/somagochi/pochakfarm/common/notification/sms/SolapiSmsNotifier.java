package com.somagochi.pochakfarm.common.notification.sms;

import com.somagochi.pochakfarm.common.notification.BulkSmsNotification;
import com.somagochi.pochakfarm.common.notification.Notification;
import com.somagochi.pochakfarm.common.notification.NotificationResult;
import com.somagochi.pochakfarm.common.notification.Notifier;
import com.somagochi.pochakfarm.common.notification.SmsNotification;
import com.somagochi.pochakfarm.common.properties.SolapiProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
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
public class SolapiSmsNotifier implements Notifier {

  private static final String SEND_PATH = "/messages/v4/send";
  private static final String SEND_MANY_PATH = "/messages/v4/send-many/detail";
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final int SALT_LENGTH_BYTES = 16;

  private final SolapiProperties properties;
  private final RestClient restClient;
  private final SecureRandom secureRandom = new SecureRandom();

  @Autowired
  public SolapiSmsNotifier(SolapiProperties properties) {
    this(properties, RestClient.builder());
  }

  SolapiSmsNotifier(SolapiProperties properties, RestClient.Builder restClientBuilder) {
    this.properties = properties;
    this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
  }

  @Override
  public boolean supports(Notification notification) {
    return notification instanceof SmsNotification || notification instanceof BulkSmsNotification;
  }

  @Override
  public NotificationResult notify(Notification notification) {
    validateConfigured();
    if (notification instanceof BulkSmsNotification bulk) {
      return sendMany(bulk);
    }
    return sendOne((SmsNotification) notification);
  }

  private NotificationResult sendOne(SmsNotification sms) {
    restClient
        .post()
        .uri(SEND_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION, createAuthorizationHeader())
        .body(SolapiSendRequest.of(sms.to(), properties.from(), sms.text()))
        .retrieve()
        .toBodilessEntity();
    log.info("solapi_sms_sent textLength={}", sms.text().length());
    return NotificationResult.success();
  }

  private NotificationResult sendMany(BulkSmsNotification bulk) {
    List<SolapiSendRequest.SolapiMessage> messages =
        bulk.messages().stream()
            .map(
                sms -> new SolapiSendRequest.SolapiMessage(sms.to(), properties.from(), sms.text()))
            .toList();
    SolapiSendManyResponse response =
        restClient
            .post()
            .uri(SEND_MANY_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, createAuthorizationHeader())
            .body(new SolapiSendManyRequest(messages))
            .retrieve()
            .body(SolapiSendManyResponse.class);
    List<String> failedRecipients =
        response == null
            ? List.<String>of()
            : response.failures().stream().map(SolapiSendManyResponse.FailedMessage::to).toList();
    log.info(
        "solapi_sms_sent_many total={} failed={}", bulk.messages().size(), failedRecipients.size());
    return new NotificationResult(failedRecipients);
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
