package com.somagochi.pochakfarm.auth.application;

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
import org.springframework.stereotype.Service;

@Service
public class SocialLoginService {

  private final SocialLoginResolver socialLoginResolver;
  private final UserRegistrationService userRegistrationService;
  private final TokenService tokenService;

  public SocialLoginService(
      SocialLoginResolver socialLoginResolver,
      UserRegistrationService userRegistrationService,
      TokenService tokenService) {
    this.socialLoginResolver = socialLoginResolver;
    this.userRegistrationService = userRegistrationService;
    this.tokenService = tokenService;
  }

  public SocialLoginResponse login(SocialLoginRequest request) {
    SocialProvider provider = SocialProvider.from(request.provider());
    validateToken(request.token());

    SocialUserInfo userInfo = socialLoginResolver.fetchUserInfo(provider, request.token());
    UserRegistration registration = userRegistrationService.getOrRegister(userInfo);
    User user = registration.user();

    TokenResponse tokenResponse = tokenService.generateTokenPair(String.valueOf(user.getId()));
    return new SocialLoginResponse(tokenResponse, registration.isNew());
  }

  private void validateToken(String token) {
    if (token == null || token.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
    }
  }
}
