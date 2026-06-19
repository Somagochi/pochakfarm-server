package com.somagochi.pochakfarm.auth.application;

import com.somagochi.pochakfarm.auth.dto.SocialLoginRequest;
import com.somagochi.pochakfarm.auth.dto.TokenResponse;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialLoginResolver;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import com.somagochi.pochakfarm.user.application.UserService;
import com.somagochi.pochakfarm.user.domain.User;
import org.springframework.stereotype.Service;

@Service
public class SocialLoginService {

  private final SocialLoginResolver socialLoginResolver;
  private final UserService userService;
  private final TokenService tokenService;

  public SocialLoginService(
      SocialLoginResolver socialLoginResolver, UserService userService, TokenService tokenService) {
    this.socialLoginResolver = socialLoginResolver;
    this.userService = userService;
    this.tokenService = tokenService;
  }

  public TokenResponse login(SocialLoginRequest request) {
    SocialProvider provider = SocialProvider.from(request.provider());
    validateToken(request.token());

    SocialUserInfo userInfo = socialLoginResolver.fetchUserInfo(provider, request.token());
    User user = userService.getOrRegister(userInfo);

    return tokenService.generateTokenPair(String.valueOf(user.getId()));
  }

  private void validateToken(String token) {
    if (token == null || token.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
    }
  }
}
