package com.somagochi.pochakfarm.animal.dto;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동물 상세 정보")
public record AnimalDetailResponse(
    @Schema(description = "동물 ID", example = "1") Long animalId,
    @Schema(description = "동물 이름", example = "솜구름") String animalName,
    @Schema(description = "카드 타입", example = "SEA") CardType cardType,
    @Schema(description = "티어", example = "A") Tier tier,
    @Schema(description = "첫 번째 스킬", example = "SEA_WAVE_DASH") CardSkill skill1,
    @Schema(description = "두 번째 스킬", example = "SEA_BUBBLE_GUARD") CardSkill skill2,
    @Schema(description = "카드 이미지 URL", example = "https://cdn.example.com/card.png")
        String cardImageUrl,
    @Schema(description = "누끼 동물 이미지 URL", example = "https://cdn.example.com/animal.png")
        String animalImageUrl) {}
