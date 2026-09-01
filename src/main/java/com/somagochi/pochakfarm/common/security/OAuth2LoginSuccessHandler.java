package com.somagochi.pochakfarm.common.security;

import com.somagochi.pochakfarm.auth.application.SocialLoginService;
import com.somagochi.pochakfarm.auth.dto.SocialLoginRequest;
import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.common.properties.OAuth2LoginProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private static final String ACCESS_TOKEN_PARAMETER = "accessToken";
  private static final String REFRESH_TOKEN_PARAMETER = "refreshToken";
  private static final String IS_NEW_PARAMETER = "isNew";
  private static final String TERMS_AGREEMENT_REQUIRED_PARAMETER = "termsAgreementRequired";

  private final SocialLoginService socialLoginService;
  private final String successRedirectUri;

  public OAuth2LoginSuccessHandler(
      SocialLoginService socialLoginService, OAuth2LoginProperties oAuth2LoginProperties) {
    this.socialLoginService = socialLoginService;
    this.successRedirectUri = requireConfigured(oAuth2LoginProperties.successRedirectUri());
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    OAuth2User principal = (OAuth2User) authentication.getPrincipal();
    String provider =
        (String) principal.getAttributes().get(OAuth2UserServiceImpl.PROVIDER_ATTRIBUTE);
    String token =
        (String) principal.getAttributes().get(OAuth2UserServiceImpl.ACCESS_TOKEN_ATTRIBUTE);

    SocialLoginResponse loginResponse =
        socialLoginService.login(new SocialLoginRequest(provider, token));

    response.sendRedirect(buildRedirectUri(loginResponse));
  }

  private String buildRedirectUri(SocialLoginResponse loginResponse) {
    return UriComponentsBuilder.fromUriString(successRedirectUri)
        .queryParam(ACCESS_TOKEN_PARAMETER, loginResponse.token().accessToken())
        .queryParam(REFRESH_TOKEN_PARAMETER, loginResponse.token().refreshToken())
        .queryParam(IS_NEW_PARAMETER, loginResponse.isNew())
        .queryParam(TERMS_AGREEMENT_REQUIRED_PARAMETER, loginResponse.termsAgreementRequired())
        .encode()
        .toUriString();
  }

  private String requireConfigured(String redirectUri) {
    if (redirectUri == null || redirectUri.isBlank()) {
      throw new IllegalArgumentException(
          "spring.security.oauth2.login.success-redirect-uri must be configured");
    }
    return redirectUri;
  }
}
