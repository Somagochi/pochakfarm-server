package com.somagochi.pochakfarm.common.notification.sms;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.somagochi.pochakfarm.common.notification.BulkSmsNotification;
import com.somagochi.pochakfarm.common.notification.Notification;
import com.somagochi.pochakfarm.common.notification.NotificationResult;
import com.somagochi.pochakfarm.common.notification.SmsNotification;
import com.somagochi.pochakfarm.common.properties.SolapiProperties;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class SolapiSmsNotifierTest {

  private static final String SEND_URL = "https://api.solapi.com/messages/v4/send";
  private static final String AUTHORIZATION_PATTERN =
      "HMAC-SHA256 apiKey=test-api-key, date=.+, salt=[0-9a-f]{32}, signature=[0-9a-f]{64}";

  private MockRestServiceServer server;

  @Test
  void supportsOnlySmsNotification() {
    SolapiSmsNotifier notifier =
        notifier(properties("test-api-key", "test-api-secret", "0212345678"));

    Assertions.assertTrue(notifier.supports(new SmsNotification("01012345678", "text")));
    Assertions.assertTrue(
        notifier.supports(
            new BulkSmsNotification(List.of(new SmsNotification("01012345678", "text")))));
    Assertions.assertFalse(notifier.supports(new Notification() {}));
  }

  @Test
  void sendsManyMessagesAndReturnsFailedRecipients() {
    SolapiSmsNotifier notifier =
        notifier(properties("test-api-key", "test-api-secret", "0212345678"));
    server
        .expect(requestTo("https://api.solapi.com/messages/v4/send-many/detail"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", Matchers.matchesPattern(AUTHORIZATION_PATTERN)))
        .andExpect(jsonPath("$.messages[0].to").value("01011112222"))
        .andExpect(jsonPath("$.messages[0].text").value("쿠폰 코드: AAAAAA"))
        .andExpect(jsonPath("$.messages[1].to").value("01033334444"))
        .andExpect(jsonPath("$.messages[1].text").value("쿠폰 코드: BBBBBB"))
        .andRespond(
            withSuccess(
                "{\"groupInfo\":{\"count\":{\"total\":2}},\"failedMessageList\":[{\"to\":\"01033334444\",\"statusCode\":\"1026\",\"statusMessage\":\"invalid\"}]}",
                MediaType.APPLICATION_JSON));

    NotificationResult result =
        notifier.notify(
            new BulkSmsNotification(
                List.of(
                    new SmsNotification("01011112222", "쿠폰 코드: AAAAAA"),
                    new SmsNotification("01033334444", "쿠폰 코드: BBBBBB"))));

    server.verify();
    Assertions.assertEquals(List.of("01033334444"), result.failedRecipients());
  }

  @Test
  void sendsMessageWithHmacAuthorizationHeader() {
    SolapiSmsNotifier notifier =
        notifier(properties("test-api-key", "test-api-secret", "0212345678"));
    server
        .expect(requestTo(SEND_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", Matchers.matchesPattern(AUTHORIZATION_PATTERN)))
        .andExpect(jsonPath("$.message.to").value("01012345678"))
        .andExpect(jsonPath("$.message.from").value("0212345678"))
        .andExpect(jsonPath("$.message.text").value("쿠폰 코드: AAAAAA"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    notifier.notify(new SmsNotification("01012345678", "쿠폰 코드: AAAAAA"));

    server.verify();
  }

  @Test
  void throwsWhenServerRespondsWithError() {
    SolapiSmsNotifier notifier =
        notifier(properties("test-api-key", "test-api-secret", "0212345678"));
    server.expect(requestTo(SEND_URL)).andRespond(withServerError());

    assertThrows(
        RestClientResponseException.class,
        () -> notifier.notify(new SmsNotification("01012345678", "쿠폰 코드: AAAAAA")));
  }

  @Test
  void throwsWhenCredentialsAreNotConfigured() {
    SolapiSmsNotifier notifier = notifier(properties("", "", "0212345678"));

    assertThrows(
        IllegalStateException.class,
        () -> notifier.notify(new SmsNotification("01012345678", "text")));
    server.verify();
  }

  @Test
  void throwsWhenFromNumberIsNotConfigured() {
    SolapiSmsNotifier notifier = notifier(properties("test-api-key", "test-api-secret", ""));

    assertThrows(
        IllegalStateException.class,
        () -> notifier.notify(new SmsNotification("01012345678", "text")));
    server.verify();
  }

  private SolapiSmsNotifier notifier(SolapiProperties properties) {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    return new SolapiSmsNotifier(properties, builder);
  }

  private SolapiProperties properties(String apiKey, String apiSecret, String from) {
    return new SolapiProperties("https://api.solapi.com", apiKey, apiSecret, from);
  }
}
