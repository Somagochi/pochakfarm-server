package com.somagochi.pochakfarm.common.social.oauth2.apple;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

public class AppleOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

  private static final String RESPONSE_MODE_PARAMETER = "response_mode";
  private static final String FORM_POST_RESPONSE_MODE = "form_post";

  private final OAuth2AuthorizationRequestResolver delegate;

  public AppleOAuth2AuthorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository,
      String authorizationRequestBaseUri) {
    this.delegate =
        new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, authorizationRequestBaseUri);
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
    return applyFormPost(delegate.resolve(request));
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
    return applyFormPost(delegate.resolve(request, registrationId));
  }

  private OAuth2AuthorizationRequest applyFormPost(
      OAuth2AuthorizationRequest authorizationRequest) {
    if (authorizationRequest == null || !isApple(authorizationRequest)) {
      return authorizationRequest;
    }
    Map<String, Object> additionalParameters =
        new HashMap<>(authorizationRequest.getAdditionalParameters());
    additionalParameters.put(RESPONSE_MODE_PARAMETER, FORM_POST_RESPONSE_MODE);
    return OAuth2AuthorizationRequest.from(authorizationRequest)
        .additionalParameters(additionalParameters)
        .build();
  }

  private boolean isApple(OAuth2AuthorizationRequest authorizationRequest) {
    String registrationId = authorizationRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID);
    return SocialProvider.APPLE.name().equalsIgnoreCase(registrationId);
  }
}
