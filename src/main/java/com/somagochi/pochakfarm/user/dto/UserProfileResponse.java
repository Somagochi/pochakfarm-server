package com.somagochi.pochakfarm.user.dto;

import com.somagochi.pochakfarm.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserProfileResponse(
    @Schema(description = "닉네임", example = "포착이") String nickname,
    @Schema(description = "레벨", example = "3") int level,
    @Schema(description = "보유 코인", example = "1200") long coins) {

  public static UserProfileResponse from(User user) {
    return new UserProfileResponse(user.getNickname(), user.getLevel(), user.getCoins());
  }
}
