package com.somagochi.pochakfarm.common.social.oidc.apple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.common.social.oauth2.apple.AppleClientSecretGenerator;
import com.somagochi.pochakfarm.common.social.oauth2.apple.AppleClientSecretParametersConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.MultiValueMap;

class AppleClientSecretParametersConverterTest {

  private final AppleClientSecretGenerator appleClientSecretGenerator =
      mock(AppleClientSecretGenerator.class);
  private final AppleClientSecretParametersConverter converter =
      new AppleClientSecretParametersConverter(appleClientSecretGenerator);

  @Test
  void replacesClientSecretForAppleRegistration() {
    given(appleClientSecretGenerator.generate()).willReturn("generated-client-secret");

    MultiValueMap<String, String> parameters =
        converter.convert(AppleGrantRequests.of("apple", "auth-code"));

    assertEquals(1, parameters.size());
    assertEquals(1, parameters.get(OAuth2ParameterNames.CLIENT_SECRET).size());
    assertEquals(
        "generated-client-secret", parameters.getFirst(OAuth2ParameterNames.CLIENT_SECRET));
  }

  @Test
  void leavesOtherRegistrationsUntouched() {
    assertNull(converter.convert(AppleGrantRequests.of("kakao", "auth-code")));
    assertNull(converter.convert(AppleGrantRequests.of("naver", "auth-code")));
  }
}
