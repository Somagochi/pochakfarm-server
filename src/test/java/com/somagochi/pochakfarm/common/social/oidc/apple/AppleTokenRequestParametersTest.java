package com.somagochi.pochakfarm.common.social.oidc.apple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.somagochi.pochakfarm.common.social.oauth2.apple.AppleClientSecretGenerator;
import com.somagochi.pochakfarm.common.social.oauth2.apple.AppleClientSecretParametersConverter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AppleTokenRequestParametersTest {

  private static final String TOKEN_RESPONSE =
      """
      {"access_token":"apple-access-token","token_type":"Bearer","expires_in":3600,\
      "id_token":"apple-id-token"}
      """;

  private final AppleClientSecretGenerator appleClientSecretGenerator =
      mock(AppleClientSecretGenerator.class);

  private RestClientAuthorizationCodeTokenResponseClient tokenResponseClient;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder =
        RestClient.builder()
            .configureMessageConverters(
                converters ->
                    converters
                        .disableDefaults()
                        .addCustomConverter(new FormHttpMessageConverter())
                        .addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter()));
    server = MockRestServiceServer.bindTo(builder).build();

    tokenResponseClient = new RestClientAuthorizationCodeTokenResponseClient();
    tokenResponseClient.setRestClient(builder.build());
    tokenResponseClient.setParametersConverter(
        new AppleClientSecretParametersConverter(appleClientSecretGenerator));
  }

  @Test
  void sendsGeneratedClientSecretExactlyOnceForApple() {
    given(appleClientSecretGenerator.generate()).willReturn("generated-client-secret");
    List<String> bodies = new ArrayList<>();

    server
        .expect(requestTo(AppleGrantRequests.TOKEN_URI))
        .andExpect(method(HttpMethod.POST))
        .andExpect(request -> bodies.add(bodyOf(request)))
        .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

    OAuth2AccessTokenResponse response =
        tokenResponseClient.getTokenResponse(AppleGrantRequests.of("apple", "auth-code"));

    server.verify();
    String body = bodies.getFirst();
    assertEquals(1, countOf(body, OAuth2ParameterNames.CLIENT_SECRET));
    assertTrue(body.contains("client_secret=generated-client-secret"), body);
    assertTrue(!body.contains(AppleGrantRequests.STATIC_CLIENT_SECRET), body);
    assertEquals("apple-access-token", response.getAccessToken().getTokenValue());
    assertEquals("apple-id-token", response.getAdditionalParameters().get("id_token"));
  }

  @Test
  void keepsStaticClientSecretForOtherProviders() {
    List<String> bodies = new ArrayList<>();

    server
        .expect(requestTo(AppleGrantRequests.TOKEN_URI))
        .andExpect(request -> bodies.add(bodyOf(request)))
        .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

    tokenResponseClient.getTokenResponse(AppleGrantRequests.of("kakao", "auth-code"));

    server.verify();
    String body = bodies.getFirst();
    assertEquals(1, countOf(body, OAuth2ParameterNames.CLIENT_SECRET));
    assertTrue(body.contains("client_secret=" + AppleGrantRequests.STATIC_CLIENT_SECRET), body);
  }

  private static String bodyOf(org.springframework.http.client.ClientHttpRequest request) {
    return URLDecoder.decode(
        ((MockClientHttpRequest) request).getBodyAsString(), StandardCharsets.UTF_8);
  }

  private static int countOf(String body, String parameterName) {
    return body.split(parameterName + "=", -1).length - 1;
  }
}
