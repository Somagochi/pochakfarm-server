package com.somagochi.pochakfarm.common.social.oauth2.apple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class AppleOAuth2AuthorizationRequestResolverTest {

  private static final String BASE_URI = "/api/auth/oauth2";

  private final AppleOAuth2AuthorizationRequestResolver resolver =
      new AppleOAuth2AuthorizationRequestResolver(
          new InMemoryClientRegistrationRepository(
              List.of(registration("apple"), registration("kakao"))),
          BASE_URI);

  @Test
  void addsFormPostResponseModeForApple() {
    OAuth2AuthorizationRequest authorizationRequest = resolver.resolve(requestFor("apple"));

    assertEquals("form_post", authorizationRequest.getAdditionalParameters().get("response_mode"));
    assertTrue(
        authorizationRequest.getAuthorizationRequestUri().contains("response_mode=form_post"),
        authorizationRequest.getAuthorizationRequestUri());
  }

  @Test
  void keepsRequiredAuthorizationParametersForApple() {
    String uri = resolver.resolve(requestFor("apple")).getAuthorizationRequestUri();

    assertTrue(uri.startsWith("https://appleid.apple.com/auth/authorize?"), uri);
    assertEquals(1, countOf(uri, "response_type=code"));
    assertEquals(1, countOf(uri, "response_mode=form_post"));
    assertTrue(uri.contains("client_id=com.somagochi.pochakfarm.web"), uri);
    assertTrue(uri.contains("scope=email"), uri);
    assertTrue(uri.contains("state="), uri);
  }

  @Test
  void leavesOtherProvidersUntouched() {
    OAuth2AuthorizationRequest authorizationRequest = resolver.resolve(requestFor("kakao"));

    assertFalse(authorizationRequest.getAdditionalParameters().containsKey("response_mode"));
    assertFalse(
        authorizationRequest.getAuthorizationRequestUri().contains("response_mode"),
        authorizationRequest.getAuthorizationRequestUri());
  }

  @Test
  void returnsNullWhenRequestDoesNotMatchAuthorizationEndpoint() {
    assertNull(resolver.resolve(new MockHttpServletRequest("GET", "/api/auth/login")));
  }

  private static MockHttpServletRequest requestFor(String registrationId) {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", BASE_URI + "/" + registrationId);
    request.setServletPath(BASE_URI + "/" + registrationId);
    return request;
  }

  private static ClientRegistration registration(String registrationId) {
    return ClientRegistration.withRegistrationId(registrationId)
        .clientId("com.somagochi.pochakfarm.web")
        .clientSecret("client-secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://api.example.com/api/auth/oauth2/code/" + registrationId)
        .scope("email")
        .authorizationUri("https://appleid.apple.com/auth/authorize")
        .tokenUri("https://appleid.apple.com/auth/token")
        .build();
  }

  private static int countOf(String uri, String parameter) {
    return uri.split(parameter, -1).length - 1;
  }
}
