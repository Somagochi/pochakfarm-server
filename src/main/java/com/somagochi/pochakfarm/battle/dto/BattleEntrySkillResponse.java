package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.SkillBattleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "출전 동물의 스킬 정보")
public record BattleEntrySkillResponse(
    @Schema(description = "스킬 코드", example = "SEA_WAVE_DASH") CardSkill skill,
    @Schema(description = "스킬 이름", example = "파도 돌진") String name,
    @Schema(description = "스킬 전투 유형", example = "GAMBLE") SkillBattleType battleType,
    @Schema(description = "발동 확률(%)", example = "30") int triggerPercentage,
    @Schema(description = "발동 성공 시 획득하는 승부 포인트", example = "3") int point) {

  public static BattleEntrySkillResponse of(CardSkill skill, BattlePolicy battlePolicy) {
    return new BattleEntrySkillResponse(
        skill,
        skill.displayName(),
        skill.battleType(),
        battlePolicy.skillTriggerPercentage(skill.battleType()),
        battlePolicy.skillMoveDistance(skill.battleType()));
  }
}
