package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.SkillBattleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관장 동물의 공개 스킬 정보")
public record BattleNpcSkillResponse(
    @Schema(description = "스킬 이름", example = "구름 숨기") String name,
    @Schema(description = "스킬 전투 유형", example = "STABLE") SkillBattleType battleType) {

  public static BattleNpcSkillResponse from(CardSkill skill) {
    return new BattleNpcSkillResponse(skill.displayName(), skill.battleType());
  }
}
