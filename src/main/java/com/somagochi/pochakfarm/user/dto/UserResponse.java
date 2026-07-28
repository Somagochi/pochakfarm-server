package com.somagochi.pochakfarm.user.dto;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
    @Schema(description = "회원 이메일", example = "user@example.com") String email,
    @Schema(description = "가입한 소셜 제공자", example = "KAKAO") SocialProvider provider,
    @Schema(description = "닉네임", example = "포착이") String nickname) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getEmail(), user.getSocialAccount().getProvider(), user.getNickname());
  }
}
