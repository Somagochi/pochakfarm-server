package com.somagochi.pochakfarm.capture.dto;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record CaptureGameResultResponse(
    Long captureId,
    GameStatus gameStatus,
    @Schema(nullable = true) Reward reward,
    Progression progression) {

  public static CaptureGameResultResponse from(
      Capture capture, ProgressionState before, ProgressionState after) {
    Long grantedExperience = capture.getGrantedExperience();
    return new CaptureGameResultResponse(
        capture.getId(),
        capture.getGameStatus(),
        grantedExperience == null ? null : new Reward(grantedExperience),
        new Progression(before, after));
  }

  public record Reward(long experienceReward) {}

  public record Progression(
      @Schema(nullable = true) ProgressionState before, ProgressionState after) {}

  public record ProgressionState(int level, long experience, long requiredExperienceForNextLevel) {}
}
