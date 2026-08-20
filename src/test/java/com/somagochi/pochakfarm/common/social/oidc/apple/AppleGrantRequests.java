package com.somagochi.pochakfarm.common.social.oidc.apple;

import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;

final class AppleGrantRequests {

  static final String TOKEN_URI = "https://appleid.apple.com/auth/token";
  static final String REDIRECT_URI = "https://api.example.com/api/auth/oauth2/code/apple";
  static final String CLIENT_ID = "com.somagochi.pochakfarm.web";
  static final String STATIC_CLIENT_SECRET = "static-placeholder-secret";

  private AppleGrantRequests() {}

  static OAuth2AuthorizationCodeGrantRequest of(String registrationId, String code) {
    ClientRegistration registration =
        ClientRegistration.withRegistrationId(registrationId)
            .clientId(CLIENT_ID)
            .clientSecret(STATIC_CLIENT_SECRET)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(REDIRECT_URI)
            .scope("email")
            .authorizationUri("https://appleid.apple.com/auth/authorize")
            .tokenUri(TOKEN_URI)
            .build();

    OAuth2AuthorizationRequest authorizationRequest =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://appleid.apple.com/auth/authorize")
            .clientId(CLIENT_ID)
            .redirectUri(REDIRECT_URI)
            .state("state-value")
            .build();

    OAuth2AuthorizationResponse authorizationResponse =
        OAuth2AuthorizationResponse.success(code)
            .redirectUri(REDIRECT_URI)
            .state("state-value")
            .build();

    return new OAuth2AuthorizationCodeGrantRequest(
        registration, new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse));
  }
}
