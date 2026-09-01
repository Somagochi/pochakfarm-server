package com.somagochi.pochakfarm.common.social.oidc.apple;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.AppleProperties;
import com.somagochi.pochakfarm.common.social.SocialLoginClient;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import com.somagochi.pochakfarm.common.social.oidc.OidcVerifier;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

@Component
public class AppleSocialLoginClient implements SocialLoginClient {

  private final OidcVerifier oidcVerifier;
  private final AppleProperties appleProperties;

  public AppleSocialLoginClient(OidcVerifier oidcVerifier, AppleProperties appleProperties) {
    this.oidcVerifier = oidcVerifier;
    this.appleProperties = requireAudiencesConfigured(appleProperties);
  }

  private AppleProperties requireAudiencesConfigured(AppleProperties appleProperties) {
    if (appleProperties.audiences() == null || appleProperties.audiences().isEmpty()) {
      throw new IllegalArgumentException("app.social.apple.audiences must be configured");
    }
    return appleProperties;
  }

  @Override
  public boolean supports(SocialProvider provider) {
    return provider == SocialProvider.APPLE;
  }

  @Override
  public SocialUserInfo authenticate(String token) {
    Claims claims =
        oidcVerifier.verify(
            token,
            appleProperties.issuer(),
            appleProperties.audiences(),
            appleProperties.jwksUri());
    String subject = claims.getSubject();
    if (subject == null) {
      throw new BusinessException(ErrorCode.SOCIAL_USER_INFO_FAILED);
    }
    return new SocialUserInfo(SocialProvider.APPLE, subject, claims.get("email", String.class));
  }
}
