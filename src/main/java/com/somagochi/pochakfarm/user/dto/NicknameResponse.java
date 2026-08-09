package com.somagochi.pochakfarm.user.dto;

import com.somagochi.pochakfarm.user.domain.User;

public record NicknameResponse(String nickname) {

  public static NicknameResponse from(User user) {
    return new NicknameResponse(user.getNickname());
  }
}
