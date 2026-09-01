package com.somagochi.pochakfarm.common.social.oauth2.apple;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class AppleClientSecretParametersConverter
    implements Converter<OAuth2AuthorizationCodeGrantRequest, MultiValueMap<String, String>> {

  private final AppleClientSecretGenerator appleClientSecretGenerator;

  public AppleClientSecretParametersConverter(
      AppleClientSecretGenerator appleClientSecretGenerator) {
    this.appleClientSecretGenerator = appleClientSecretGenerator;
  }

  @Override
  public MultiValueMap<String, String> convert(OAuth2AuthorizationCodeGrantRequest source) {
    if (!isApple(source)) {
      return null;
    }
    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.set(OAuth2ParameterNames.CLIENT_SECRET, appleClientSecretGenerator.generate());
    return parameters;
  }

  private boolean isApple(OAuth2AuthorizationCodeGrantRequest source) {
    return SocialProvider.APPLE
        .name()
        .equalsIgnoreCase(source.getClientRegistration().getRegistrationId());
  }
}
