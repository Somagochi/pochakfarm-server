package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "관장 동물 정보")
public record GymLeaderAnimalResponse(
    @Schema(description = "출전 순서", example = "1") int orderNo,
    @Schema(description = "동물 이름", example = "별콩") String animalName,
    @Schema(description = "카드 타입", example = "SPACE") CardType cardType,
    @Schema(description = "티어", example = "B") Tier tier,
    @Schema(description = "동물 이미지 URL. 에셋 미확정이면 null") String animalImageUrl,
    @Schema(description = "보유 스킬 2개. 이름과 전투 유형만 공개한다") List<BattleNpcSkillResponse> skills) {}
