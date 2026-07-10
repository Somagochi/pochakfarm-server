package com.somagochi.pochakfarm.common.social;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SocialLoginResolver {

  private final List<SocialLoginClient> socialLoginClients;

  public SocialLoginResolver(List<SocialLoginClient> socialLoginClients) {
    this.socialLoginClients = socialLoginClients;
  }

  public SocialUserInfo fetchUserInfo(SocialProvider provider, String token) {
    return resolveClient(provider).authenticate(token);
  }

  private SocialLoginClient resolveClient(SocialProvider provider) {
    return socialLoginClients.stream()
        .filter(client -> client.supports(provider))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER));
  }
}
