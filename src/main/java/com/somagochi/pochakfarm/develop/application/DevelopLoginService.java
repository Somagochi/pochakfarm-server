package com.somagochi.pochakfarm.develop.application;

import com.somagochi.pochakfarm.auth.application.TokenService;
import com.somagochi.pochakfarm.auth.dto.SocialLoginResponse;
import com.somagochi.pochakfarm.auth.dto.TokenResponse;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import com.somagochi.pochakfarm.user.application.UserRegistrationService;
import com.somagochi.pochakfarm.user.dto.UserRegistration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "dev"})
public class DevelopLoginService {

  private static final SocialProvider DEV_PROVIDER = SocialProvider.KAKAO;

  private final UserRegistrationService userRegistrationService;
  private final TokenService tokenService;

  public DevelopLoginService(
      UserRegistrationService userRegistrationService, TokenService tokenService) {
    this.userRegistrationService = userRegistrationService;
    this.tokenService = tokenService;
  }

  public SocialLoginResponse login(Long userId) {
    UserRegistration registration = userRegistrationService.getOrRegister(toDevUserInfo(userId));
    TokenResponse token =
        tokenService.generateTokenPair(String.valueOf(registration.user().getId()));
    return new SocialLoginResponse(
        token, registration.isNew(), registration.user().isTermsAgreementRequired());
  }

  private SocialUserInfo toDevUserInfo(Long userId) {
    String providerId = "dev-" + userId;
    return new SocialUserInfo(DEV_PROVIDER, providerId, providerId + "@pochakfarm.dev");
  }
}
