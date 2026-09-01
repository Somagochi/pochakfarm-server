package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.social.oauth2.apple.AppleClientSecretGenerator;
import com.somagochi.pochakfarm.common.social.oauth2.apple.AppleClientSecretParametersConverter;
import com.somagochi.pochakfarm.common.social.oauth2.apple.AppleOAuth2AuthorizationRequestResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

@Configuration
public class AppleOAuth2ClientConfig {

  public static final String AUTHORIZATION_REQUEST_BASE_URI = "/api/auth/oauth2";

  @Bean
  OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient(
      AppleClientSecretGenerator appleClientSecretGenerator) {
    RestClientAuthorizationCodeTokenResponseClient tokenResponseClient =
        new RestClientAuthorizationCodeTokenResponseClient();
    tokenResponseClient.setParametersConverter(
        new AppleClientSecretParametersConverter(appleClientSecretGenerator));
    return tokenResponseClient;
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "spring.security.oauth2.client.registration.apple",
      name = "client-id")
  OAuth2AuthorizationRequestResolver authorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository) {
    return new AppleOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository, AUTHORIZATION_REQUEST_BASE_URI);
  }
}
