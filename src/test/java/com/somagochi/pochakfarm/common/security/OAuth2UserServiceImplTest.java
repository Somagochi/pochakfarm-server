package com.somagochi.pochakfarm.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

class OAuth2UserServiceImplTest {

  private final OAuth2UserServiceImpl oauth2UserService = new OAuth2UserServiceImpl();

  @Test
  void usesIdTokenForApple() {
    OAuth2User user =
        oauth2UserService.loadUser(userRequest("apple", Map.of("id_token", "apple-id-token")));

    assertEquals("apple", user.getAttributes().get(OAuth2UserServiceImpl.PROVIDER_ATTRIBUTE));
    assertEquals(
        "apple-id-token", user.getAttributes().get(OAuth2UserServiceImpl.ACCESS_TOKEN_ATTRIBUTE));
  }

  @Test
  void usesAccessTokenForOtherProviders() {
    OAuth2User user = oauth2UserService.loadUser(userRequest("kakao", Map.of()));

    assertEquals("kakao", user.getAttributes().get(OAuth2UserServiceImpl.PROVIDER_ATTRIBUTE));
    assertEquals(
        "provider-access-token",
        user.getAttributes().get(OAuth2UserServiceImpl.ACCESS_TOKEN_ATTRIBUTE));
  }

  @Test
  void throwsWhenAppleResponseHasNoIdToken() {
    OAuth2UserRequest userRequest = userRequest("apple", Map.of());

    assertThrows(
        OAuth2AuthenticationException.class, () -> oauth2UserService.loadUser(userRequest));
  }

  private static OAuth2UserRequest userRequest(
      String registrationId, Map<String, Object> additionalParameters) {
    ClientRegistration registration =
        ClientRegistration.withRegistrationId(registrationId)
            .clientId("client-id")
            .clientSecret("client-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://api.example.com/api/auth/oauth2/code/" + registrationId)
            .authorizationUri("https://example.com/authorize")
            .tokenUri("https://example.com/token")
            .build();
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "provider-access-token",
            Instant.now(),
            Instant.now().plusSeconds(3600));
    return new OAuth2UserRequest(registration, accessToken, additionalParameters);
  }
}
