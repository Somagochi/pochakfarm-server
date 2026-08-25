package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.characterization.domain.SkillBattleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 동물 스킬 정보")
public record BattleSkillResponse(
    @Schema(description = "스킬 이름", example = "궤도 걸음") String name,
    @Schema(description = "스킬 전투 유형", example = "BALANCED") SkillBattleType battleType,
    @Schema(description = "발동 확률(%)", example = "45") int triggerPercentage,
    @Schema(description = "발동 성공 시 획득하는 승부 포인트", example = "2") int point) {}
