package com.somagochi.pochakfarm.common.social.oidc.apple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.somagochi.pochakfarm.common.properties.AppleProperties;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import com.somagochi.pochakfarm.common.social.oidc.OidcVerifier;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppleSocialLoginClientTest {

  private static final String ISSUER = "https://appleid.apple.com";
  private static final String JWKS_URI = "https://appleid.apple.com/auth/keys";
  private static final String APP_AUDIENCE = "com.example.app";
  private static final String WEB_AUDIENCE = "com.example.app.web";
  private static final String SUBJECT = "apple-user-123";

  private final OidcVerifier oidcVerifier = mock(OidcVerifier.class);

  @Test
  void verifiesTokenAgainstEveryConfiguredAudience() {
    AppleProperties properties =
        new AppleProperties(ISSUER, List.of(APP_AUDIENCE, WEB_AUDIENCE), JWKS_URI);
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(SUBJECT);
    when(oidcVerifier.verify(
            eq("id-token"), eq(ISSUER), eq(List.of(APP_AUDIENCE, WEB_AUDIENCE)), eq(JWKS_URI)))
        .thenReturn(claims);

    SocialUserInfo userInfo =
        new AppleSocialLoginClient(oidcVerifier, properties).authenticate("id-token");

    assertEquals(SocialProvider.APPLE, userInfo.provider());
    assertEquals(SUBJECT, userInfo.providerId());
  }

  @Test
  void throwsWhenAudiencesNotConfigured() {
    AppleProperties properties = new AppleProperties(ISSUER, List.of(), JWKS_URI);

    assertThrows(
        IllegalArgumentException.class, () -> new AppleSocialLoginClient(oidcVerifier, properties));
  }
}
