package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 출전 동물 정보")
public record BattleUserEntryResponse(
    @Schema(description = "동물 ID", example = "31") Long animalId,
    @Schema(description = "출전 순서", example = "1") int orderNo,
    @Schema(description = "동물 이름", example = "솜구름") String animalName,
    @Schema(description = "카드 타입", example = "SKY") CardType cardType,
    @Schema(description = "티어", example = "A") Tier tier,
    @Schema(description = "첫 번째 스킬") BattleSkillResponse skill1,
    @Schema(description = "두 번째 스킬") BattleSkillResponse skill2) {}
