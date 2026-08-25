package com.somagochi.pochakfarm.animal.dto;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "동물(캡처) 정보")
public record AnimalResponse(
    @Schema(description = "동물 ID", example = "1") Long animalId,
    @Schema(description = "동물 이름", example = "솜구름") String animalName,
    @Schema(description = "카드 타입", example = "SEA") CardType cardType,
    @Schema(description = "티어", example = "A") Tier tier,
    @Schema(description = "카드 이미지 URL", example = "https://cdn.example.com/card.png")
        String cardImageUrl,
    @Schema(description = "누끼 동물 이미지 URL", example = "https://cdn.example.com/animal.png")
        String animalImageUrl,
    @Schema(description = "관장전 휴식 종료 시각. 휴식 중이 아니면 null", example = "2026-08-25T06:30:00Z")
        Instant restEndsAt) {}
