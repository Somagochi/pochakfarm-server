package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관장 동물 정보. 스킬 정보는 대전 전에 공개하지 않는다")
public record GymLeaderAnimalResponse(
    @Schema(description = "출전 순서", example = "1") int orderNo,
    @Schema(description = "동물 이름", example = "별콩") String animalName,
    @Schema(description = "카드 타입", example = "SPACE") CardType cardType,
    @Schema(description = "티어", example = "B") Tier tier,
    @Schema(description = "동물 이미지 URL. 에셋 미확정이면 null") String animalImageUrl) {}
