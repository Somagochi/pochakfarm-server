package com.somagochi.pochakfarm.animal.dto;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동물 스킬 정보")
public record AnimalSkillResponse(
    @Schema(description = "스킬 이름", example = "파도 돌진") String name,
    @Schema(description = "스킬 설명", example = "시원한 파도를 타고 힘차게 달려들어요.") String description) {

  public static AnimalSkillResponse from(CardSkill skill) {
    if (skill == null) {
      return null;
    }
    return new AnimalSkillResponse(skill.displayName(), skill.description());
  }
}
