package com.somagochi.pochakfarm.common.security;

import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuth2UserServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  public static final String PROVIDER_ATTRIBUTE = "provider";
  public static final String ACCESS_TOKEN_ATTRIBUTE = "accessToken";

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    String provider = userRequest.getClientRegistration().getRegistrationId();
    String accessToken = userRequest.getAccessToken().getTokenValue();

    Map<String, Object> attributes =
        Map.of(PROVIDER_ATTRIBUTE, provider, ACCESS_TOKEN_ATTRIBUTE, accessToken);
    return new DefaultOAuth2User(List.of(), attributes, PROVIDER_ATTRIBUTE);
  }
}
