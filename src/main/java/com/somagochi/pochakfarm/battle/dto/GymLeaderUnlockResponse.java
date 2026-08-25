package com.somagochi.pochakfarm.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관장 해금 조건과 충족 여부")
public record GymLeaderUnlockResponse(
    @Schema(description = "해금 여부. 두 조건을 모두 충족해야 true", example = "false") boolean unlocked,
    @Schema(description = "요구 유저 레벨", example = "12") int requiredLevel,
    @Schema(description = "요구 레벨 충족 여부", example = "true") boolean levelSatisfied,
    @Schema(description = "직전 관장 뱃지 코드. 1번 관장은 null", example = "BDG008") String previousBadgeCode,
    @Schema(description = "직전 관장 뱃지 보유 여부. 1번 관장은 항상 true", example = "false")
        boolean previousBadgeSatisfied) {

  public static GymLeaderUnlockResponse of(
      int requiredLevel, boolean levelSatisfied, String previousBadgeCode, boolean badgeSatisfied) {
    return new GymLeaderUnlockResponse(
        levelSatisfied && badgeSatisfied,
        requiredLevel,
        levelSatisfied,
        previousBadgeCode,
        badgeSatisfied);
  }
}
