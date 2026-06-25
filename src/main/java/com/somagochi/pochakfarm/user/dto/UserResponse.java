package com.somagochi.pochakfarm.user.dto;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;

public record UserResponse(Long id, String email, SocialProvider provider) {

  public static UserResponse from(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getSocialAccount().getProvider());
  }
}
