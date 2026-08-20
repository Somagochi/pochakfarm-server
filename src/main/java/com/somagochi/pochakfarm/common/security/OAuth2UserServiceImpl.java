package com.somagochi.pochakfarm.common.security;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuth2UserServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  public static final String PROVIDER_ATTRIBUTE = "provider";
  public static final String ACCESS_TOKEN_ATTRIBUTE = "accessToken";

  private static final String MISSING_ID_TOKEN_ERROR = "missing_id_token";

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    String provider = userRequest.getClientRegistration().getRegistrationId();

    Map<String, Object> attributes =
        Map.of(PROVIDER_ATTRIBUTE, provider, ACCESS_TOKEN_ATTRIBUTE, resolveToken(userRequest));
    return new DefaultOAuth2User(List.of(), attributes, PROVIDER_ATTRIBUTE);
  }

  private String resolveToken(OAuth2UserRequest userRequest) {
    if (!isApple(userRequest)) {
      return userRequest.getAccessToken().getTokenValue();
    }
    Object idToken = userRequest.getAdditionalParameters().get(OidcParameterNames.ID_TOKEN);
    if (idToken == null) {
      throw new OAuth2AuthenticationException(new OAuth2Error(MISSING_ID_TOKEN_ERROR));
    }
    return String.valueOf(idToken);
  }

  private boolean isApple(OAuth2UserRequest userRequest) {
    return SocialProvider.APPLE
        .name()
        .equalsIgnoreCase(userRequest.getClientRegistration().getRegistrationId());
  }
}
