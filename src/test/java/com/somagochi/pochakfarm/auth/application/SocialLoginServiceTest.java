package com.somagochi.pochakfarm.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.somagochi.pochakfarm.auth.dto.SocialLoginRequest;
import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.auth.dto.TokenResponse;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialLoginResolver;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import com.somagochi.pochakfarm.user.application.UserRegistrationService;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.UserRegistration;
import org.junit.jupiter.api.Test;

class SocialLoginServiceTest {

  private final SocialLoginResolver socialLoginResolver = mock(SocialLoginResolver.class);
  private final UserRegistrationService userRegistrationService =
      mock(UserRegistrationService.class);
  private final TokenService tokenService = mock(TokenService.class);
  private final SocialLoginService socialLoginService =
      new SocialLoginService(socialLoginResolver, userRegistrationService, tokenService);

  @Test
  void issuesTokenPairForUserIdentifiedBySocialProvider() {
    SocialLoginRequest request = new SocialLoginRequest("kakao", "social-token");
    SocialUserInfo userInfo =
        new SocialUserInfo(SocialProvider.KAKAO, "kakao-123", "user@kakao.com");
    User user = mock(User.class);
    given(user.getId()).willReturn(1L);
    given(user.isTermsAgreementRequired()).willReturn(true);
    given(socialLoginResolver.fetchUserInfo(SocialProvider.KAKAO, "social-token"))
        .willReturn(userInfo);
    given(userRegistrationService.getOrRegister(userInfo))
        .willReturn(new UserRegistration(user, true));
    given(tokenService.generateTokenPair("1")).willReturn(new TokenResponse("access", "refresh"));

    SocialLoginResponse response = socialLoginService.login(request);

    assertEquals("access", response.token().accessToken());
    assertEquals("refresh", response.token().refreshToken());
    assertEquals(true, response.isNew());
    assertEquals(true, response.termsAgreementRequired());
    verify(tokenService).generateTokenPair("1");
  }

  @Test
  void requiresTermsAgreementOnSubsequentLoginUntilAgreementIsCompleted() {
    SocialLoginRequest request = new SocialLoginRequest("kakao", "social-token");
    SocialUserInfo userInfo =
        new SocialUserInfo(SocialProvider.KAKAO, "kakao-123", "user@kakao.com");
    User user = mock(User.class);
    given(user.getId()).willReturn(1L);
    given(user.isTermsAgreementRequired()).willReturn(true);
    given(socialLoginResolver.fetchUserInfo(SocialProvider.KAKAO, "social-token"))
        .willReturn(userInfo);
    given(userRegistrationService.getOrRegister(userInfo))
        .willReturn(new UserRegistration(user, false));
    given(tokenService.generateTokenPair("1")).willReturn(new TokenResponse("access", "refresh"));

    SocialLoginResponse response = socialLoginService.login(request);

    assertEquals(false, response.isNew());
    assertEquals(true, response.termsAgreementRequired());
  }

  @Test
  void throwsWhenProviderIsUnsupported() {
    SocialLoginRequest request = new SocialLoginRequest("unknown", "social-token");

    BusinessException exception =
        assertThrows(BusinessException.class, () -> socialLoginService.login(request));

    assertEquals(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER.getCode(), exception.getCode());
    verifyNoInteractions(socialLoginResolver, userRegistrationService, tokenService);
  }

  @Test
  void throwsWhenTokenIsBlank() {
    SocialLoginRequest request = new SocialLoginRequest("kakao", " ");

    BusinessException exception =
        assertThrows(BusinessException.class, () -> socialLoginService.login(request));

    assertEquals(ErrorCode.INVALID_SOCIAL_TOKEN.getCode(), exception.getCode());
    verifyNoInteractions(socialLoginResolver, userRegistrationService, tokenService);
  }

  @Test
  void throwsWhenTokenIsNull() {
    SocialLoginRequest request = new SocialLoginRequest("kakao", null);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> socialLoginService.login(request));

    assertEquals(ErrorCode.INVALID_SOCIAL_TOKEN.getCode(), exception.getCode());
    verifyNoInteractions(socialLoginResolver, userRegistrationService, tokenService);
  }
}
