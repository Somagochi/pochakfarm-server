package com.somagochi.pochakfarm.characterization.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CharacterizationResponse(
    @Schema(description = "생성된 캐릭터라이징 ID", example = "1") Long characterizationId,
    @Schema(
            description = "S3에 저장된 결과 이미지 접근 URL",
            example = "https://cdn.example.com/public/characterization-result/result.png")
        String resultImageUrl,
    @Schema(
            description = "S3에 저장된 카드 뒷면 이미지 접근 URL",
            example = "https://cdn.example.com/public/characterization-back/back.png")
        String cardBackImageUrl) {}
