package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.battle.domain.SkillActivationResult;
import com.somagochi.pochakfarm.battle.domain.SkillActivationStatus;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.SkillBattleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "한쪽 진영의 스킬 판정 결과")
public record BattleSkillOutcomeResponse(
    @Schema(description = "선택한 스킬. 미선택이면 null", example = "SEA_WAVE_DASH") CardSkill skill,
    @Schema(description = "선택한 스킬 이름. 미선택이면 null", example = "파도 돌진") String skillName,
    @Schema(description = "선택한 스킬의 전투 유형. 미선택이면 null", example = "GAMBLE")
        SkillBattleType battleType,
    @Schema(
            description = "판정 상태. NOT_SELECTED(미선택), ACTIVATED(발동 성공), FAILED(선택했지만 발동 실패)",
            example = "ACTIVATED")
        SkillActivationStatus status,
    @Schema(description = "이 진영이 획득한 승부 포인트. 미선택과 발동 실패는 0", example = "3") int point) {

  public static BattleSkillOutcomeResponse from(SkillActivationResult result) {
    return new BattleSkillOutcomeResponse(
        result.skill().orElse(null),
        result.skill().map(CardSkill::displayName).orElse(null),
        result.battleType(),
        result.status(),
        result.points());
  }
}
