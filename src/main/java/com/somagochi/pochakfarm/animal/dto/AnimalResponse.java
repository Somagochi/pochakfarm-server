package com.somagochi.pochakfarm.animal.dto;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동물 정보")
public record AnimalResponse(
    @Schema(description = "동물 ID", example = "1") Long animalId,
    @Schema(description = "동물 이름", example = "솜구름") String animalName,
    @Schema(description = "생성 작업 상태", example = "SUCCEEDED") CharacterizationStatus status,
    @Schema(
            description = "카드 이미지 접근 URL",
            example = "https://cdn.example.com/public/animal-card/card.png")
        String cardImageUrl,
    @Schema(
            description = "누끼를 딴 동물 이미지 접근 URL",
            example = "https://cdn.example.com/public/animal-cutout/animal.png")
        String animalImageUrl,
    @Schema(description = "카드 타입", example = "GROUND") CardType cardType,
    @Schema(description = "실패 사유 코드. 실패 상태가 아니면 null", example = "CHARACTERIZATION_FAILED")
        String failureReason) {}
