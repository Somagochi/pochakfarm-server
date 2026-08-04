package com.somagochi.pochakfarm.common.sms;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.somagochi.pochakfarm.common.properties.SolapiProperties;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class SolapiSmsSenderTest {

  private static final String SEND_URL = "https://api.solapi.com/messages/v4/send";
  private static final String AUTHORIZATION_PATTERN =
      "HMAC-SHA256 apiKey=test-api-key, date=.+, salt=[0-9a-f]{32}, signature=[0-9a-f]{64}";

  private MockRestServiceServer server;

  @Test
  void sendsMessageWithHmacAuthorizationHeader() {
    SolapiSmsSender sender = sender(properties("test-api-key", "test-api-secret", "0212345678"));
    server
        .expect(requestTo(SEND_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", Matchers.matchesPattern(AUTHORIZATION_PATTERN)))
        .andExpect(jsonPath("$.message.to").value("01012345678"))
        .andExpect(jsonPath("$.message.from").value("0212345678"))
        .andExpect(jsonPath("$.message.text").value("쿠폰 코드: AAAAAA"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    sender.send("01012345678", "쿠폰 코드: AAAAAA");

    server.verify();
  }

  @Test
  void throwsWhenServerRespondsWithError() {
    SolapiSmsSender sender = sender(properties("test-api-key", "test-api-secret", "0212345678"));
    server.expect(requestTo(SEND_URL)).andRespond(withServerError());

    assertThrows(
        RestClientResponseException.class, () -> sender.send("01012345678", "쿠폰 코드: AAAAAA"));
  }

  @Test
  void throwsWhenCredentialsAreNotConfigured() {
    SolapiSmsSender sender = sender(properties("", "", "0212345678"));

    assertThrows(IllegalStateException.class, () -> sender.send("01012345678", "text"));
    server.verify();
  }

  @Test
  void throwsWhenFromNumberIsNotConfigured() {
    SolapiSmsSender sender = sender(properties("test-api-key", "test-api-secret", ""));

    assertThrows(IllegalStateException.class, () -> sender.send("01012345678", "text"));
    server.verify();
  }

  private SolapiSmsSender sender(SolapiProperties properties) {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    return new SolapiSmsSender(properties, builder);
  }

  private SolapiProperties properties(String apiKey, String apiSecret, String from) {
    return new SolapiProperties("https://api.solapi.com", apiKey, apiSecret, from);
  }
}
