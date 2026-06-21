package com.somagochi.pochakfarm.common.social.oauth2.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverUserResponse(String resultcode, String message, NaverAccount response) {

  public String id() {
    return response == null ? null : response.id();
  }

  public String email() {
    return response == null ? null : response.email();
  }

  public String nickname() {
    return response == null ? null : response.nickname();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record NaverAccount(String id, String email, String nickname) {}
}
