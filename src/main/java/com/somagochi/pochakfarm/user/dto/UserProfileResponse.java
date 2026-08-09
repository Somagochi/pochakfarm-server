package com.somagochi.pochakfarm.user.dto;

import com.somagochi.pochakfarm.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserProfileResponse(
    @Schema(description = "닉네임", example = "포착이") String nickname,
    @Schema(description = "레벨", example = "3") int level,
    @Schema(description = "보유 코인", example = "1200") long coins,
    @Schema(description = "현재 레벨에서 보유한 경험치", example = "54") long currentExperience,
    @Schema(description = "다음 레벨 달성에 필요한 전체 경험치", example = "60") long requiredExperience,
    @Schema(description = "레벨업까지 남은 경험치", example = "6") long remainingExperience) {

  public static UserProfileResponse from(
      User user, long requiredExperience, long remainingExperience) {
    return new UserProfileResponse(
        user.getNickname(),
        user.getLevel(),
        user.getCoins(),
        user.getExperience(),
        requiredExperience,
        remainingExperience);
  }
}
