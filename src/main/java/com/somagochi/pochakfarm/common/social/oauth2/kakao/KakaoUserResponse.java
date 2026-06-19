package com.somagochi.pochakfarm.common.social.oauth2.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserResponse(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {

  public String email() {
    return kakaoAccount == null ? null : kakaoAccount.email();
  }

  public String nickname() {
    if (kakaoAccount == null || kakaoAccount.profile() == null) {
      return null;
    }
    return kakaoAccount.profile().nickname();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record KakaoAccount(String email, Profile profile) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(String nickname) {}
  }
}
