package com.somagochi.pochakfarm.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.auth.application.SocialLoginService;
import com.somagochi.pochakfarm.auth.dto.SocialLoginRequest;
import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.auth.dto.TokenResponse;
import com.somagochi.pochakfarm.common.properties.OAuth2LoginProperties;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

class OAuth2LoginSuccessHandlerTest {

  private static final String REDIRECT_URI = "https://front.example.com/coupon";

  private final SocialLoginService socialLoginService = mock(SocialLoginService.class);

  private final OAuth2LoginSuccessHandler successHandler =
      new OAuth2LoginSuccessHandler(
          socialLoginService, new OAuth2LoginProperties(REDIRECT_URI, Duration.ofMinutes(3)));

  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @Test
  void redirectsToConfiguredUriWithIssuedTokens() throws IOException {
    given(socialLoginService.login(new SocialLoginRequest("apple", "apple-id-token")))
        .willReturn(
            new SocialLoginResponse(
                new TokenResponse("access-token", "refresh-token"), true, true));

    successHandler.onAuthenticationSuccess(
        new MockHttpServletRequest(), response, authentication("apple", "apple-id-token"));

    assertEquals(302, response.getStatus());
    MultiValueMap<String, String> parameters = queryOf(response.getRedirectedUrl());
    assertTrue(response.getRedirectedUrl().startsWith(REDIRECT_URI + "?"));
    assertEquals("access-token", parameters.getFirst("accessToken"));
    assertEquals("refresh-token", parameters.getFirst("refreshToken"));
    assertEquals("true", parameters.getFirst("isNew"));
    assertEquals("true", parameters.getFirst("termsAgreementRequired"));
  }

  @Test
  void passesProviderAndTokenFromPrincipalToSocialLogin() throws IOException {
    given(socialLoginService.login(new SocialLoginRequest("kakao", "kakao-access-token")))
        .willReturn(
            new SocialLoginResponse(
                new TokenResponse("access-token", "refresh-token"), false, false));
    ArgumentCaptor<SocialLoginRequest> loginRequest =
        ArgumentCaptor.forClass(SocialLoginRequest.class);

    successHandler.onAuthenticationSuccess(
        new MockHttpServletRequest(), response, authentication("kakao", "kakao-access-token"));

    verify(socialLoginService).login(loginRequest.capture());
    assertEquals("kakao", loginRequest.getValue().provider());
    assertEquals("kakao-access-token", loginRequest.getValue().token());
  }

  @Test
  void encodesTokensSoReservedCharactersCannotSplitQueryParameters() throws IOException {
    given(socialLoginService.login(new SocialLoginRequest("apple", "apple-id-token")))
        .willReturn(new SocialLoginResponse(new TokenResponse("a&b=c", "d&e=f"), false, false));

    successHandler.onAuthenticationSuccess(
        new MockHttpServletRequest(), response, authentication("apple", "apple-id-token"));

    String redirectedUrl = response.getRedirectedUrl();
    assertTrue(redirectedUrl.contains("accessToken=a%26b%3Dc"), redirectedUrl);
    assertTrue(redirectedUrl.contains("refreshToken=d%26e%3Df"), redirectedUrl);
    assertEquals(4, queryOf(redirectedUrl).size());
  }

  @Test
  void rejectsBlankRedirectUri() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new OAuth2LoginSuccessHandler(
                socialLoginService, new OAuth2LoginProperties("  ", Duration.ofMinutes(3))));
  }

  private static Authentication authentication(String provider, String token) {
    DefaultOAuth2User principal =
        new DefaultOAuth2User(
            List.of(),
            Map.of(
                OAuth2UserServiceImpl.PROVIDER_ATTRIBUTE,
                provider,
                OAuth2UserServiceImpl.ACCESS_TOKEN_ATTRIBUTE,
                token),
            OAuth2UserServiceImpl.PROVIDER_ATTRIBUTE);
    return new UsernamePasswordAuthenticationToken(principal, null, List.of());
  }

  private static MultiValueMap<String, String> queryOf(String uri) {
    return UriComponentsBuilder.fromUri(URI.create(uri)).build().getQueryParams();
  }
}
