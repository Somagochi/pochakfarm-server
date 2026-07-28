package com.somagochi.pochakfarm.farm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "슬롯에 배치된 동물 정보")
public record FarmAnimalResponse(
    @Schema(description = "동물(캐릭터라이징) ID", example = "11") Long animalId,
    @Schema(description = "동물 이름", example = "솜구름") String animalName,
    @Schema(description = "카드 이미지 URL", example = "https://cdn.example.com/a.png")
        String cardImageUrl,
    @Schema(description = "누끼를 딴 이미지 URL", example = "https://cdn.example.com/a-cutout.png")
        String animalImageUrl) {}
