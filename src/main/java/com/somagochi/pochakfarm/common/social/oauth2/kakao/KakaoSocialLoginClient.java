package com.somagochi.pochakfarm.common.social.oauth2.kakao;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialLoginClient;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import com.somagochi.pochakfarm.common.properties.KakaoProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoSocialLoginClient implements SocialLoginClient {

  private static final String BEARER_PREFIX = "Bearer ";

  private final RestClient restClient;
  private final String userInfoPath;

  public KakaoSocialLoginClient(KakaoProperties kakaoProperties) {
    this.restClient = RestClient.builder().baseUrl(kakaoProperties.baseUrl()).build();
    this.userInfoPath = kakaoProperties.userInfoPath();
  }

  @Override
  public boolean supports(SocialProvider provider) {
    return provider == SocialProvider.KAKAO;
  }

  @Override
  public SocialUserInfo authenticate(String token) {
    KakaoUserResponse response = requestUserInfo(token);
    if (response == null || response.id() == null) {
      throw new BusinessException(ErrorCode.SOCIAL_USER_INFO_FAILED);
    }
    return new SocialUserInfo(SocialProvider.KAKAO, String.valueOf(response.id()), response.email());
  }

  private KakaoUserResponse requestUserInfo(String token) {
    try {
      return restClient
          .get()
          .uri(userInfoPath)
          .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
          .retrieve()
          .onStatus(
              HttpStatusCode::is4xxClientError,
              (request, clientResponse) -> {
                throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
              })
          .onStatus(
              HttpStatusCode::is5xxServerError,
              (request, clientResponse) -> {
                throw new BusinessException(ErrorCode.SOCIAL_USER_INFO_FAILED);
              })
          .body(KakaoUserResponse.class);
    } catch (RestClientException exception) {
      throw new BusinessException(ErrorCode.SOCIAL_USER_INFO_FAILED);
    }
  }
}
