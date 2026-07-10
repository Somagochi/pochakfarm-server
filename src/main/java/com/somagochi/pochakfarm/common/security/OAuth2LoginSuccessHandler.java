package com.somagochi.pochakfarm.common.security;

import com.somagochi.pochakfarm.auth.application.SocialLoginService;
import com.somagochi.pochakfarm.auth.dto.SocialLoginRequest;
import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final SocialLoginService socialLoginService;
  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  public OAuth2LoginSuccessHandler(SocialLoginService socialLoginService) {
    this.socialLoginService = socialLoginService;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    OAuth2User principal = (OAuth2User) authentication.getPrincipal();
    String provider =
        (String) principal.getAttributes().get(OAuth2UserServiceImpl.PROVIDER_ATTRIBUTE);
    String accessToken =
        (String) principal.getAttributes().get(OAuth2UserServiceImpl.ACCESS_TOKEN_ATTRIBUTE);

    SocialLoginResponse loginResponse =
        socialLoginService.login(new SocialLoginRequest(provider, accessToken));
    writeResponse(response, ApiResponse.success(loginResponse));
  }

  private void writeResponse(HttpServletResponse response, ApiResponse<SocialLoginResponse> body)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    jsonMapper.writeValue(response.getWriter(), body);
  }
}
