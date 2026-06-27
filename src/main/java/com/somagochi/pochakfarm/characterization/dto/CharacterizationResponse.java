package com.somagochi.pochakfarm.characterization.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CharacterizationResponse(
    @Schema(description = "변환 처리 결과 상태", example = "success") String status,
    @Schema(description = "실제 변환을 수행한 내부 provider", example = "codex_exec") String provider,
    @Schema(
            description = "fallback provider가 사용된 경우 원래 provider",
            nullable = true,
            example = "primary")
        String fallbackFrom,
    @Schema(description = "요청에서 전달한 반려동물 이름", example = "솜구름") String animalName,
    @Schema(
            description = "S3에 저장된 결과 이미지 접근 URL",
            example = "https://cdn.example.com/public/characterization-result/result.png")
        String resultImageUrl,
    @Schema(description = "Python 변환 서버가 보고한 처리 시간(ms)", example = "12345") Integer elapsedMs) {}
