package com.somagochi.pochakfarm.common.social.oauth2.naver;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.NaverProperties;
import com.somagochi.pochakfarm.common.social.SocialLoginClient;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.common.social.SocialUserInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NaverSocialLoginClient implements SocialLoginClient {

  private static final String BEARER_PREFIX = "Bearer ";

  private final RestClient restClient;
  private final String userInfoPath;

  public NaverSocialLoginClient(NaverProperties naverProperties) {
    this.restClient = RestClient.builder().baseUrl(naverProperties.baseUrl()).build();
    this.userInfoPath = naverProperties.userInfoPath();
  }

  @Override
  public boolean supports(SocialProvider provider) {
    return provider == SocialProvider.NAVER;
  }

  @Override
  public SocialUserInfo authenticate(String token) {
    NaverUserResponse response = requestUserInfo(token);
    if (response == null || response.id() == null) {
      throw new BusinessException(ErrorCode.SOCIAL_USER_INFO_FAILED);
    }
    return new SocialUserInfo(SocialProvider.NAVER, response.id(), response.email());
  }

  private NaverUserResponse requestUserInfo(String token) {
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
          .body(NaverUserResponse.class);
    } catch (RestClientException exception) {
      throw new BusinessException(ErrorCode.SOCIAL_USER_INFO_FAILED);
    }
  }
}
