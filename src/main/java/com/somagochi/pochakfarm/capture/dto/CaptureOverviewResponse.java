package com.somagochi.pochakfarm.capture.dto;

import com.somagochi.pochakfarm.capture.domain.TierProbability;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CaptureOverviewResponse(
    Level level, List<CaptureCount> captureCounts, List<TierProbability> tierProbabilities) {

  public record Level(
      @Schema(description = "현재 레벨", example = "12") int currentLevel,
      @Schema(description = "현재 레벨에서 보유한 경험치", example = "54") long currentExperience,
      @Schema(description = "다음 레벨까지 필요한 전체 경험치", example = "150") long requiredExperience,
      @Schema(description = "레벨업까지 남은 경험치", example = "96") long remainingExperience) {}

  public record CaptureCount(
      @Schema(description = "카드 타입", example = "SKY") CardType cardType,
      @Schema(description = "누적 포착 성공 횟수", example = "23") long count) {}
}
