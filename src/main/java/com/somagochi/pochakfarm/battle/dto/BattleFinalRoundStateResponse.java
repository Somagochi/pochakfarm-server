package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "최종 승부 진행 상태")
public record BattleFinalRoundStateResponse(
    @Schema(description = "최종 승부 진입 여부") boolean required,
    @Schema(description = "최종 승부 시작 여부") boolean started,
    @Schema(description = "최종 승부 시작 API 호출 마감 시각") Instant startExpiresAt,
    @Schema(description = "3초 탭 입력 마감 시각") Instant inputExpiresAt,
    @Schema(description = "통신 유예 1초를 포함한 결과 제출 마감 시각") Instant submissionExpiresAt,
    @Schema(description = "제출된 탭 횟수") Integer tapCount,
    @Schema(description = "탭 횟수로 환산한 승부 포인트") Integer point) {

  public static BattleFinalRoundStateResponse from(Battle battle, BattlePolicy policy) {
    if (battle.getFinalReadyAt() == null) {
      return new BattleFinalRoundStateResponse(false, false, null, null, null, null, null);
    }
    Instant inputExpiresAt = battle.getFinalExpiresAt();
    return new BattleFinalRoundStateResponse(
        true,
        inputExpiresAt != null,
        battle.getFinalReadyAt().plus(policy.finalRoundStartTimeout()),
        inputExpiresAt,
        inputExpiresAt == null ? null : inputExpiresAt.plus(policy.finalRoundSubmissionGrace()),
        battle.getFinalTapCount(),
        battle.getFinalPoints());
  }
}
